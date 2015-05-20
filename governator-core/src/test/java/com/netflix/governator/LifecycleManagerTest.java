package com.netflix.governator;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import junit.framework.Assert;

import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

@Test(singleThreaded=true)
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
        injector.addListener(new DefaultLifecycleListener() {
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
    
    @Singleton
    public static class Ready extends DefaultLifecycleListener {
        private static boolean isReady = false;

        @Inject
        public Ready(LifecycleManager manager) {
        }
        
        @Override
        public void onReady() {
            isReady = true;
        }
    }
    
    @Test
    public void testOnReadyListener() {
        LifecycleInjector injector = Governator.createInjector(new LifecycleModule(), new AbstractModule() {
            @Override
            protected void configure() {
                Multibinder.newSetBinder(binder(), LifecycleListener.class).addBinding().to(Ready.class);
            }
        });
        
        Assert.assertTrue(Ready.isReady);
    }
}
