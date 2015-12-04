package com.netflix.governator;

/**
 * Listener for Injector lifecycle events.   LifecycleListener should be 
 * reserved for framework use only.  All application shutdown 
 * code should use @PreDestroy, which is triggered via a LifecycleListener
 * enabled by installing {@link LifecycleModule}.
 * 
 * When writing a LifecycleListener that is managed by Guice, make sure to 
 * inject all dependencies lazily using {@link Provider} injection.  Otherwise,
 * these dependencies will be instantiated too early thereby bypassing lifecycle 
 * features in LifecycleModule.
 */
public interface LifecycleListener {
    /**
     * Notification that the Injector has been created.  
     */
    public void onStarted();

    /**
     * Notification that the injector is shutting down
     */
    public void onStopped();
    
    /**
     * Notification that the Injector failed to be created.  This will only be called
     * for LifecycleListeners registered with LifecycleManager before the injector 
     * is created
     */
    public void onStartFailed(Throwable t);
    
    /**
     * Notification that the injector has shutdown either due to failure or being stopped normally.
     * This will be called in addition to onStopped() and onStartFailed()
     */
    public void onFinished();
}
