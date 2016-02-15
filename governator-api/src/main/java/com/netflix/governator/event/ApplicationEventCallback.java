package com.netflix.governator.event;

public interface ApplicationEventCallback<E extends ApplicationEvent> {
    void onEvent(E event);
}
