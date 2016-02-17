package com.netflix.governator.event;

import java.lang.reflect.Method;

/**
 * Interface for publishing {@link ApplicationEvent}s as well as programmatically registering
 * {@link ApplicationEventListener}s.
 */
public interface ApplicationEventDispatcher {

    <T extends ApplicationEvent> ApplicationEventRegistration registerListener(Class<T> eventType, ApplicationEventListener<T> eventListener);

    ApplicationEventRegistration registerListener(ApplicationEventListener<? extends ApplicationEvent> eventListener);

    ApplicationEventRegistration registerListener(Object instance, Method method, Class<? extends ApplicationEvent> acceptedType);

    void publishEvent(ApplicationEvent event);

}
