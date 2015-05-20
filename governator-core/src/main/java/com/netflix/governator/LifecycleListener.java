package com.netflix.governator;

/**
 * Contract for a callback to be invoked once the Injector is shut down.
 * LifecycleListener should be reserved for framework use only.  All application 
 * code should use @PreDestroy, which is triggered via a LifecycleListener
 * enabled by installing {@link LifecycleModule}.
 * 
 * @author elandau
 */
public interface LifecycleListener {
    public void onShutdown();
}
