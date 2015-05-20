package com.netflix.governator;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import junit.framework.Assert;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.governator.LifecycleListener;
import com.netflix.governator.LifecycleManager;
import com.netflix.governator.guice.InjectorLifecycle;
import com.netflix.governator.guice.LifecycleModule;

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
        
        Injector injector = Guice.createInjector(new LifecycleModule());
        InjectorLifecycle.onShutdown(injector,  new LifecycleListener() {
            @Override
            public void onShutdown() {
                isShutdown.set(true);
            }
        });
        
        Assert.assertFalse(isShutdown.get());
        InjectorLifecycle.shutdown(injector);
        Assert.assertTrue(isShutdown.get());
    }

    @Test(timeout=1000)
    public void testWaitForInternalShutdownTrigger() throws InterruptedException {
        Injector injector = Guice.createInjector(new LifecycleModule());
        injector.getInstance(ShutdownDelay.class);
        InjectorLifecycle.awaitTermination(injector);
    }
}
