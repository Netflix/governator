package com.netflix.governator;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import junit.framework.Assert;

import org.testng.annotations.Test;

public class LifecycleManagerTest {
    @Singleton
    private static class ShutdownDelay {
        @Inject
        public ShutdownDelay(final LifecycleManager manager) {
            Executors.newScheduledThreadPool(1).schedule(new Runnable() {
                @Override
                public void run() {
                    manager.shutdown();
                }
            }, 10, TimeUnit.MILLISECONDS);
        }
    }
    
    @Test
    public void testWithExternalListener() throws InterruptedException {
        final AtomicBoolean isShutdown = new AtomicBoolean(false);
        
        LifecycleInjector injector = Governator.createInjector(new LifecycleModule());
        injector.addListener(new LifecycleListener() {
            @Override
            public void onShutdown() {
                isShutdown.set(true);
            }
        });
        
        Assert.assertFalse(isShutdown.get());
        injector.shutdown();
        Assert.assertTrue(isShutdown.get());
    }

    @Test(timeOut=1000)
    public void testWaitForInternalShutdownTrigger() throws InterruptedException {
        LifecycleInjector injector = Governator.createInjector(new LifecycleModule());
        injector.getInstance(ShutdownDelay.class);
        injector.awaitTermination();
    }
}
