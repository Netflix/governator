package com.netflix.governator;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Singleton;

import com.netflix.governator.guice.InjectorLifecycle;

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
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    
    public void addListener(LifecycleListener listener) {
        listeners.add(listener);
    }
    
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            if (listeners != null) {
                for (LifecycleListener hook : listeners) {
                    hook.onShutdown();
                }
            }
            latch.countDown();
        }
    }
    
    public void awaitTermination() throws InterruptedException {
        latch.await();
    }
}
