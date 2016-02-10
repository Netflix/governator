package com.netflix.governator.event;

/**
 * Interface to encapsulate event publication.
 */
public interface ApplicationEventPublisher {

    void publishEvent(ApplicationEvent event);
    
}
