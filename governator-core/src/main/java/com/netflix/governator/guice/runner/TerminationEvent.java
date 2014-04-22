package com.netflix.governator.guice.runner;

/**
 * Abstraction for an event that when fired should tell the LifecycleRunner 
 * to terminate.  A concrete TerminatEvent type is normally paired with a
 * specific runner implementation.
 * 
 * @author elandau
 *
 * TODO: Add additional listeners of the termination event
 */
public interface TerminationEvent {
    /**
     * Block until the termination event is fired.
     * 
     * @throws InterruptedException
     */
    public void await() throws InterruptedException;
    
    /**
     * Fire the termination event.
     */
    public void terminate();
    
    /**
     * @return True if the termination event was set.
     */
    public boolean isTerminated();
}
