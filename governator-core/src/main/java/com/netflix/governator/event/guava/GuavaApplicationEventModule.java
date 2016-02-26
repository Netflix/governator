package com.netflix.governator.event.guava;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import javax.inject.Inject;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.reflect.TypeToken;
import com.google.inject.AbstractModule;
import com.netflix.governator.event.ApplicationEvent;
import com.netflix.governator.event.ApplicationEventDispatcher;
import com.netflix.governator.event.ApplicationEventListener;
import com.netflix.governator.event.ApplicationEventModule;
import com.netflix.governator.event.ApplicationEventRegistration;

public final class GuavaApplicationEventModule extends AbstractModule {   
     
    @Override
    protected void configure() {  
        install(new ApplicationEventModule());    
        bind(ApplicationEventDispatcher.class).to(GuavaApplicationEventDispatcher.class).asEagerSingleton();
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
        return "GuavaApplicationEventModule[]";
    }
       
    private static final class GuavaApplicationEventDispatcher implements ApplicationEventDispatcher {
    
        private final EventBus eventBus;
        private final Method eventListenerMethod;
    
        @Inject
        public GuavaApplicationEventDispatcher(EventBus eventBus) {
            this.eventBus = eventBus;
            try {
                this.eventListenerMethod = ApplicationEventListener.class.getDeclaredMethod("onEvent", ApplicationEvent.class);
            } catch (Exception e) {
                throw new RuntimeException("Failed to cache ApplicationEventListener method", e);
            }
        }
    
        public ApplicationEventRegistration registerListener(Object instance, Method method, Class<? extends ApplicationEvent> eventType) {
            GuavaSubscriberProxy proxy = new GuavaSubscriberProxy(instance, method, eventType);
            eventBus.register(proxy);
            return new GuavaEventRegistration(eventBus, proxy);
        }
    
        public <T extends ApplicationEvent> ApplicationEventRegistration registerListener(Class<T> eventType, ApplicationEventListener<T> eventListener) {
            GuavaSubscriberProxy proxy = new GuavaSubscriberProxy(eventListener, eventListenerMethod, eventType);
            eventBus.register(proxy);
            return new GuavaEventRegistration(eventBus, proxy);
        }

        public ApplicationEventRegistration registerListener(ApplicationEventListener<? extends ApplicationEvent> eventListener) {
            Type[] genericInterfaces = eventListener.getClass().getGenericInterfaces();
            for (Type type : genericInterfaces) {
                if (ApplicationEventListener.class.isAssignableFrom(TypeToken.of(type).getRawType())) {
                    ParameterizedType ptype = (ParameterizedType) type;
                    Class<?> rawType = TypeToken.of(ptype.getActualTypeArguments()[0]).getRawType();
                    GuavaSubscriberProxy proxy = new GuavaSubscriberProxy(eventListener, eventListenerMethod, rawType);
                    eventBus.register(proxy);
                    return new GuavaEventRegistration(eventBus, proxy);
                }
            }
            return new ApplicationEventRegistration() {
                public void unregister() {}  //no-op. Could not find anything to register.
            };
        }
    
        private static class GuavaSubscriberProxy {
    
            private final Object handlerInstance;
            private final Method handlerMethod;
            private final Class<?> acceptedType;
    
            public GuavaSubscriberProxy(Object handlerInstance, Method handlerMethod, Class<?> acceptedType) {
                this.handlerInstance = handlerInstance;
                this.handlerMethod = handlerMethod;
                this.acceptedType = acceptedType;
            }
    
            @Subscribe
            public void invokeEventHandler(ApplicationEvent event)
                    throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                if (acceptedType.isAssignableFrom(event.getClass())) {
                    if (!handlerMethod.isAccessible()) {
                        handlerMethod.setAccessible(true);
                    }
                    handlerMethod.invoke(handlerInstance, event);
                }
            }
        }
        
        private static class GuavaEventRegistration implements ApplicationEventRegistration { 
           
            private final EventBus eventBus;
            private final GuavaSubscriberProxy subscriber;
            
            public GuavaEventRegistration(EventBus eventBus, GuavaSubscriberProxy subscriber) {
                this.eventBus = eventBus;
                this.subscriber = subscriber;
            }

            public void unregister() {
                this.eventBus.unregister(subscriber);
            }
        }
    
        @Override
        public void publishEvent(ApplicationEvent event) {
            this.eventBus.post(event);
        }
    }
}
