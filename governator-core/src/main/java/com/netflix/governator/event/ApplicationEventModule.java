package com.netflix.governator.event;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

/**
 * Adds support for passing {@link ApplicationEvent}s backed by Guava {@link EventBus}
 * See {@link EventListener} and {@link ApplicationEventPublisher} for usage. 
 */
public class ApplicationEventModule extends AbstractModule {
    
    private static final Logger LOG = LoggerFactory.getLogger(ApplicationEventModule.class);
    private final EventBus eventBus = new EventBus("Governator Application EventBus");
    private final ApplicationEventSubscribingTypeListener subscribingTypeListener = new ApplicationEventSubscribingTypeListener(eventBus);
    

    @Singleton
    private static class GuavaApplicationEventPublisher implements ApplicationEventPublisher {
        
        private final EventBus eventBus;

        @Inject
        public GuavaApplicationEventPublisher(EventBus eventBus) {
            this.eventBus = eventBus;  
        }

        @Override
        public void publishEvent(ApplicationEvent event) {
            this.eventBus.post(event);
        }
        
    }
    
    @Singleton
    private static class ApplicationEventSubscribingTypeListener implements TypeListener {
       
        private EventBus eventBus;
        
        public ApplicationEventSubscribingTypeListener(EventBus eventBus) {
            this.eventBus = eventBus;
        }

        @Override
        public <I> void hear(TypeLiteral<I> type, TypeEncounter<I> encounter) {
            Class<?> clazz = type.getRawType();
            while (clazz != null && !Collection.class.isAssignableFrom(clazz) && !clazz.isArray()) {
              for (final Method method : clazz.getDeclaredMethods()) {
                  if(method.isAnnotationPresent(EventListener.class)) {
                      if(method.getReturnType().equals(Void.TYPE)
                              && method.getParameterTypes().length == 1 
                              && ApplicationEvent.class.isAssignableFrom(method.getParameterTypes()[0])) {
                            encounter.register(new InjectionListener<Object>() {
                                @Override
                                public void afterInjection(Object injectee) {
                                    GuavaSubscriberProxy proxy = new GuavaSubscriberProxy(injectee, method);
                                    eventBus.register(proxy);
                                }
                            });
                      }
                      else
                      {
                          LOG.warn("@EventListener {}.{} skipped. Methods must be public, void, and accept exactly"
                                  + " one argument extending com.netflix.governator.event.ApplicationEvent.", clazz.getName(), method.getName());
                      }
                  }
              }
              clazz = clazz.getSuperclass();   
          }
        }
    }
    
    private static class GuavaSubscriberProxy { 
        
        private final Object handlerInstance;
        private final Method handlerMethod;
        
        public GuavaSubscriberProxy(Object handlerInstance, Method handlerMethod) {
            this.handlerInstance = handlerInstance;
            this.handlerMethod = handlerMethod;
        }
        
        @Subscribe
        public void invokeEventHandler(ApplicationEvent event) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
            if(!handlerMethod.isAccessible()) {
                handlerMethod.setAccessible(true);
            }    
            handlerMethod.invoke(handlerInstance, event);           
        }
    }

    @Override
    protected void configure() {
        bind(EventBus.class).toInstance(eventBus);
        bind(ApplicationEventPublisher.class).to(GuavaApplicationEventPublisher.class);
        bindListener(Matchers.any(), subscribingTypeListener);
        
    }

}
