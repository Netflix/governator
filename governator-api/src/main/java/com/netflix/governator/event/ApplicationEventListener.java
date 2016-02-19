package com.netflix.governator.event;

/***
 * Interface for receiving to events of a given type. Can be registered explicitly
 * via {@link ApplicationEventDispatcher#registerListener(ApplicationEventListener)} 
 * or implicitly in Guice by detecting by all bindings for instances of {@link ApplicationEvent} 
 * */
public interface ApplicationEventListener<T extends ApplicationEvent> {

    public void onEvent(T event);

}
