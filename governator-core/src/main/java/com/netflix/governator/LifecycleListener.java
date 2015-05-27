package com.netflix.governator;

/**
 * Listener for Injector lifecycle events.   LifecycleListener should be 
 * reserved for framework use only.  All application shutdown 
 * code should use @PreDestroy, which is triggered via a LifecycleListener
 * enabled by installing {@link LifecycleModule}.
 * 
 * @author elandau
 */
public interface LifecycleListener {
    /**
     * Notification that the Injector failed to be created.  This will only be called
     * for LifecycleListeners registered with LifecycleManager before the injector 
     * is created
     */
    public void onStartFailed(Throwable t);
    
    /**
     * Notification that the injector is shutting down
     */
    public void onStopped();
    
    /**
     * Notification that the Injector is about to be started.  This method is only
     * called for LifecycleListeners registered with LifecycleManager before the 
     * injector is created.
     */
    public void onStarting();
    
    /**
     * Notification that the Injector has been created.  
     */
    public void onStarted();
}
