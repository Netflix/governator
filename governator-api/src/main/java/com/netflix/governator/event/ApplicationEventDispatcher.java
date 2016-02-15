package com.netflix.governator.event;

import java.lang.reflect.Method;

/**
 * Interface to publishing {@link ApplicationEvent}s as well as programmatically registering
 * {@link ApplicationEventListener}s.
 */
public interface ApplicationEventDispatcher {

    <T extends ApplicationEvent> void registerListener(Class<T> eventType, ApplicationEventListener<T> eventListener);

    void registerListener(ApplicationEventListener<? extends ApplicationEvent> eventListener);

    void registerListener(Object instance, Method method, Class<? extends ApplicationEvent> acceptedType);

    void publishEvent(ApplicationEvent event);

}
