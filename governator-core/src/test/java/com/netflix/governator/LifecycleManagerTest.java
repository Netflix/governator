package com.netflix.governator;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Singleton;

import junit.framework.Assert;

import org.testng.annotations.Test;

import com.google.inject.AbstractModule;

@Test
public class LifecycleManagerTest {
    @Singleton
    private static class ShutdownDelay {
        @Inject
        public ShutdownDelay(final LifecycleShutdownSignal event) {
            Executors.newScheduledThreadPool(1).schedule(new Runnable() {
                @Override
                public void run() {
                    event.signal();
                }
            }, 10, TimeUnit.MILLISECONDS);
        }
    }
    
    public static class CountingLifecycleListener implements LifecycleListener {
        final AtomicInteger stoppedCount = new AtomicInteger();
        final AtomicInteger startedCount = new AtomicInteger();
        final AtomicInteger startFailedCount = new AtomicInteger();
        
        @Override
        public void onStopped() {
            stoppedCount.incrementAndGet();
        }

        @Override
        public void onStarted() {
            startedCount.incrementAndGet();
        }

        @Override
        public void onStartFailed(Throwable t) {
            startFailedCount.incrementAndGet();
        }
        
        int getStartedCount() {
            return startedCount.get();
        }
        
        int getStoppedCount() {
            return stoppedCount.get();
        }
        
        int getStartFailedCount() {
            return startFailedCount.get();
        }
        
    }
    
    @Test
    public void testWithExternalListener() throws InterruptedException {
        
        LifecycleInjector injector = Governator.createInjector();
        CountingLifecycleListener listener = new CountingLifecycleListener();
        injector.addListener(listener);
        
        Assert.assertEquals(0, listener.getStartedCount());
        Assert.assertEquals(0, listener.getStartFailedCount());
        Assert.assertEquals(0, listener.getStoppedCount());
        
        injector.shutdown();
        
        Assert.assertEquals(0, listener.getStartedCount());
        Assert.assertEquals(0, listener.getStartFailedCount());
        Assert.assertEquals(1, listener.getStoppedCount());
    }

    @Test(timeOut=1000)
    public void testWaitForInternalShutdownTrigger() throws InterruptedException {
        LifecycleInjector injector = Governator.createInjector();
        injector.getInstance(ShutdownDelay.class);
        injector.awaitTermination();
    }
    
    @Test
    public void testOnReadyListener() {
        final CountingLifecycleListener listener = new CountingLifecycleListener();
        LifecycleInjector injector = Governator.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(CountingLifecycleListener.class).toInstance(listener);
            }
        });
        
        Assert.assertEquals(1, listener.getStartedCount());
        Assert.assertEquals(0, listener.getStartFailedCount());
        Assert.assertEquals(0, listener.getStoppedCount());
        
        injector.shutdown();
        
        Assert.assertEquals(1, listener.getStartedCount());
        Assert.assertEquals(0, listener.getStartFailedCount());
        Assert.assertEquals(1, listener.getStoppedCount());
    }
}
