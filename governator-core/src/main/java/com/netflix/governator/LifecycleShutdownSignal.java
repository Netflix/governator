package com.netflix.governator;

import com.google.inject.ImplementedBy;

/**
 * Shutdown signal for the lifecycle manager.  Code can either block on the signal
 * being fired or trigger it from a shutdown mechanism, such as a shutdown PID or
 * shutdown socket.  Each container is likely to have it's own implementation of
 * shutdown signal.
 * 
 * @author elandau
 *
 */
@ImplementedBy(DefaultLifecycleShutdownSignal.class)
public interface LifecycleShutdownSignal {
    /**
     * Signal shutdown
     */
    void signal();
    
    /**
     * Wait for shutdown to be signalled.  This could be either the result of 
     * calling signal() or an internal shutdown mechanism for the container.
     * 
     * @throws InterruptedException
     */
    void await() throws InterruptedException;

}
