package com.netflix.governator.event;

/**
 * Interface to unregistering a subscriber to events. Returned from {@link ApplicationEventDispatcher} 
 * whenever a received is programmatically registered. 
 */
public interface ApplicationEventRegistration {
    
    void unregister();

}
