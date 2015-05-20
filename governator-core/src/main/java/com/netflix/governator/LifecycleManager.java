package com.netflix.governator;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;

import javax.inject.Singleton;

import com.google.inject.Inject;

/**
 * @see {@link InjectorLifecycle}
 * 
 * @author elandau
 *
 */
@Singleton
public class LifecycleManager {
    private final CopyOnWriteArraySet<LifecycleListener> listeners = new CopyOnWriteArraySet<>();
    private final CountDownLatch latch = new CountDownLatch(1);
    
    @Inject(optional=true)
    public void addListeners(Set<LifecycleListener> listeners) {
        this.listeners.addAll(listeners);
    }
    
    public void addListener(LifecycleListener listener) {
        listeners.add(listener);
    }
    
    public void shutdown() {
        if (listeners != null) {
            for (LifecycleListener hook : listeners) {
                hook.onShutdown();
            }
        }
        latch.countDown();
    }
    
    public void awaitTermination() throws InterruptedException {
        latch.await();
    }
}
