package com.netflix.governator.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.ProvisionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.netflix.governator.event.guava.GuavaApplicationEventModule;

/**
 * Adds support for passing {@link ApplicationEvent}s. Default (Guava-based) implementation
 * can be found in {@link GuavaApplicationEventModule}
 * 
 * See {@link EventListener} and {@link ApplicationEventDispatcher} for usage. 
 */
public final class ApplicationEventModule extends AbstractModule {
    
    private static class ApplicationEventSubscribingTypeListener implements TypeListener {

        private static final Logger LOG = LoggerFactory.getLogger(ApplicationEventSubscribingTypeListener.class);
        private final Provider<ApplicationEventDispatcher> dispatcherProvider;
        
        public ApplicationEventSubscribingTypeListener(Provider<ApplicationEventDispatcher> dispatcherProvider) {
            this.dispatcherProvider = dispatcherProvider;
        }

        @Override
        public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            final Class<?> clazz = type.getRawType();
            final List<Method> handlerMethods = getAllDeclaredHandlerMethods(clazz);
            if(!handlerMethods.isEmpty())
            {
                encounter.register(new InjectionListener<Object>() {
                    @Override
                    public void afterInjection(Object injectee) {
                        for (final Method handlerMethod : handlerMethods) {
                            dispatcherProvider.get().registerListener(injectee, handlerMethod,
                                    (Class<? extends ApplicationEvent>) handlerMethod.getParameterTypes()[0]);
                        }
                    }
                });
            }
        }

        private List<Method> getAllDeclaredHandlerMethods(Class<?> clazz) {
            final List<Method> handlerMethods = new ArrayList<>();
            while (clazz != null && !Collection.class.isAssignableFrom(clazz) && !clazz.isArray()) {
                for (final Method handlerMethod : clazz.getDeclaredMethods()) {
                    if (handlerMethod.isAnnotationPresent(EventListener.class)) {
                        if (handlerMethod.getReturnType().equals(Void.TYPE) 
                                && handlerMethod.getParameterTypes().length == 1
                                && ApplicationEvent.class.isAssignableFrom(handlerMethod.getParameterTypes()[0])) {
                            handlerMethods.add(handlerMethod);
                        } else {
                            throw new IllegalArgumentException("@EventListener " + clazz.getName() + "." + handlerMethod.getName()
                                    + "skipped. Methods must be public, void, and accept exactly"
                                    + " one argument extending com.netflix.governator.event.ApplicationEvent.");
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
            return handlerMethods;
        }
    }
    
    private static class ApplicationEventSubscribingProvisionListener implements ProvisionListener {
        
        private final Provider<ApplicationEventDispatcher> dispatcherProvider;
        
        public ApplicationEventSubscribingProvisionListener(Provider<ApplicationEventDispatcher> dispatcherProvider) {
            this.dispatcherProvider = dispatcherProvider;
        }
        
        @Override
        public <T> void onProvision(ProvisionInvocation<T> provision) {
            T provisioned = provision.provision();
            if (provisioned != null && provisioned instanceof ApplicationEventListener) {
                dispatcherProvider.get().registerListener((ApplicationEventListener)provisioned);
            }
        }
    }

    @Override
    protected void configure() {
        com.google.inject.Provider<ApplicationEventDispatcher> dispatcherProvider = binder().getProvider(ApplicationEventDispatcher.class);
        bindListener(Matchers.any(),  new ApplicationEventSubscribingTypeListener(dispatcherProvider));
        bindListener(Matchers.any(), new ApplicationEventSubscribingProvisionListener(dispatcherProvider));
    }
    
    @Override
    public boolean equals(Object obj) {
        return getClass().equals(obj.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ApplicationEventModule[]";
    }
}
