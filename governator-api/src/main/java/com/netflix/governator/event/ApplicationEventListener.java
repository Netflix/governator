package com.netflix.governator.event;

public interface ApplicationEventListener<T extends ApplicationEvent> {
    
    public void onEvent(T event);

}
