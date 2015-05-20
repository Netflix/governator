package com.netflix.governator;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @see {@link LifecycleInjector}
 * 
 * @author elandau
 *
 */
@Singleton
public class LifecycleManager {
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleManager.class);
    
    private final CopyOnWriteArraySet<LifecycleListener> listeners = new CopyOnWriteArraySet<>();
    private final CountDownLatch latch = new CountDownLatch(1);
    private final AtomicBoolean isShutdown = new AtomicBoolean(false);
    
    public void addListener(LifecycleListener listener) {
        listeners.add(listener);
    }
    
    public void shutdown() {
        if (isShutdown.compareAndSet(false, true)) {
            LOG.info("Shutting down LifecycleManager");
            if (listeners != null) {
                for (LifecycleListener hook : listeners) {
                    try {
                        hook.onShutdown();
                    }
                    catch (Exception e) {
                        LOG.error("Failed to shutdown hook {}", hook, e);
                    }
                }
            }
            latch.countDown();
        }
        else {
            LOG.warn("LifecycleManager already shut down");
        }
    }
    
    public void awaitTermination() throws InterruptedException {
        latch.await();
    }
}
