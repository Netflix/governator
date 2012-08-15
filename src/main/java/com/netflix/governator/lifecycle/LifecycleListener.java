package com.netflix.governator.lifecycle;

/**
 * Callback for injected instances
 */
public interface LifecycleListener
{
    /**
     * When the IoC container being used injects an object, this callback will
     * be notified
     *
     * @param obj object being injected
     */
    public void     objectInjected(Object obj);

    /**
     * Called when an objects lifecycle state changes
     *
     * @param obj the object
     * @param newState new state
     */
    public void     stateChanged(Object obj, LifecycleState newState);
}
