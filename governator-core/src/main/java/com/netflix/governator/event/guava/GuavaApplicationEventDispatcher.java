package com.netflix.governator.event.guava;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import com.google.common.reflect.TypeToken;
import com.netflix.governator.event.ApplicationEvent;
import com.netflix.governator.event.ApplicationEventDispatcher;
import com.netflix.governator.event.ApplicationEventListener;

public class GuavaApplicationEventDispatcher implements ApplicationEventDispatcher {

    private final EventBus eventBus;
    private final Method eventListenerMethod;

    public GuavaApplicationEventDispatcher(EventBus eventBus) {
        this.eventBus = eventBus;
        try {
            this.eventListenerMethod = ApplicationEventListener.class.getDeclaredMethod("onEvent", ApplicationEvent.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to cache ApplicationEventListener method", e);
        }
    }

    @Override
    public void registerListener(Object instance, Method method, Class<? extends ApplicationEvent> eventType) {
        GuavaSubscriberProxy proxy = new GuavaSubscriberProxy(instance, method, eventType);
        eventBus.register(proxy);
    }

    @Override
    public <T extends ApplicationEvent> void registerListener(Class<T> eventType, ApplicationEventListener<T> eventListener) {
        GuavaSubscriberProxy proxy = new GuavaSubscriberProxy(eventListener, eventListenerMethod, eventType);
        eventBus.register(proxy);
    }

    @Override
    public void registerListener(ApplicationEventListener<? extends ApplicationEvent> eventListener) {
        Type[] genericInterfaces = eventListener.getClass().getGenericInterfaces();
        for (Type type : genericInterfaces) {
            if (ApplicationEventListener.class.isAssignableFrom(TypeToken.of(type).getRawType())) {
                ParameterizedType ptype = (ParameterizedType) type;
                Class<?> rawType = TypeToken.of(ptype.getActualTypeArguments()[0]).getRawType();
                GuavaSubscriberProxy proxy = new GuavaSubscriberProxy(eventListener, eventListenerMethod, rawType);
                eventBus.register(proxy);
            }
        }
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

    @Override
    public void publishEvent(ApplicationEvent event) {
        this.eventBus.post(event);
    }

}