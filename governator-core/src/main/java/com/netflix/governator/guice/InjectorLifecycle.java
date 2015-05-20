package com.netflix.governator.guice;

import com.google.inject.Injector;
import com.netflix.governator.LifecycleListener;
import com.netflix.governator.LifecycleManager;

/**
 * Utility class that simplifies calling LifecycleManager for a Guice Injector
 * 
 * @author elandau
 *
 */
public class InjectorLifecycle {
    /**
     * Block until LifecycleManager terminates
     * 
     * @param injector
     * @throws InterruptedException
     */
    public static void awaitTermination(Injector injector) throws InterruptedException {
        injector.getInstance(LifecycleManager.class).awaitTermination();
    }

    /**
     * Shutdown LifecycleManager on this Injector which will invoke all registered
     * {@link LifecycleListener}s and unblock awaitTermination. 
     * 
     * @param injector
     */
    public static void shutdown(Injector injector) {
        injector.getInstance(LifecycleManager.class).shutdown();
    }
    
    /**
     * Register a single shutdown listener for async notification of the LifecycleManager
     * terminating. 
     * 
     * @param injector
     * @param listener
     */
    public static void onShutdown(Injector injector, LifecycleListener listener) {
        injector.getInstance(LifecycleManager.class).addListener(listener);
    }
}
