package com.netflix.governator.event;

import java.lang.reflect.Method;

public interface ApplicationEventDispatcher {

    <T extends ApplicationEvent> void registerListener(Class<T> eventType, ApplicationEventCallback<T> callback);

    void registerListener(ApplicationEventListener<? extends ApplicationEvent> eventListener);

    void registerListener(Object instance, Method method, Class<? extends ApplicationEvent> acceptedType);

    void publishEvent(ApplicationEvent event);

}
