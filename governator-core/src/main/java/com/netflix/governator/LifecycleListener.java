package com.netflix.governator;

/**
 * Listener for Injector lifecycle events.  
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
    public void onStopped(Throwable error);
}
