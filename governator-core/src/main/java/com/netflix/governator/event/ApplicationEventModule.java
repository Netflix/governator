package com.netflix.governator.event;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
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
public class ApplicationEventModule extends AbstractModule {
    
    private final ApplicationEventSubscribingTypeListener subscribingTypeListener; 
    private final ApplicationEventDispatcher dispatcher;
    
    public ApplicationEventModule(ApplicationEventDispatcher dispatcher) {
        this.dispatcher = dispatcher;
        subscribingTypeListener = new ApplicationEventSubscribingTypeListener(dispatcher);
    }
    
    @Singleton
    private static class ApplicationEventSubscribingTypeListener implements TypeListener {

        private static final Logger LOG = LoggerFactory.getLogger(ApplicationEventModule.class);
        private final ApplicationEventDispatcher dispatcher;

        public ApplicationEventSubscribingTypeListener(ApplicationEventDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Override
        public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            final List<Method> handlerMethods = new ArrayList<>();
            Class<?> clazz = type.getRawType();
            while (clazz != null && !Collection.class.isAssignableFrom(clazz) && !clazz.isArray()) {
                for (final Method handlerMethod : clazz.getDeclaredMethods()) {
                    if (handlerMethod.isAnnotationPresent(EventListener.class)) {
                        if (handlerMethod.getReturnType().equals(Void.TYPE) 
                                && handlerMethod.getParameterTypes().length == 1
                                && ApplicationEvent.class.isAssignableFrom(handlerMethod.getParameterTypes()[0])) {
                            handlerMethods.add(handlerMethod);
                        } else {
                            LOG.warn(
                                    "@EventListener {}.{} skipped. Methods must be public, void, and accept exactly"
                                            + " one argument extending com.netflix.governator.event.ApplicationEvent.",
                                    clazz.getName(), handlerMethod.getName());
                        }
                    }
                }
                clazz = clazz.getSuperclass();
            }
            encounter.register(new InjectionListener<Object>() {
                @Override
                public void afterInjection(Object injectee) {
                    for (final Method handlerMethod : handlerMethods) {
                        dispatcher.registerListener(injectee, handlerMethod, (Class<? extends ApplicationEvent>) handlerMethod.getParameterTypes()[0]);
                    }
                }
            });
        }
    }

    @Override
    protected void configure() {
        bind(ApplicationEventDispatcher.class).toInstance(dispatcher);
        bindListener(Matchers.any(), subscribingTypeListener);
        bindListener(Matchers.any(), new ProvisionListener() {
            @Override
            public <T> void onProvision(ProvisionInvocation<T> provision) {
                T provisioned = provision.provision();
                if (provisioned instanceof ApplicationEventListener) {
                    dispatcher.registerListener((ApplicationEventListener) provisioned);
                }
            }
        });
    }
}
