package com.netflix.governator;

import java.util.concurrent.CountDownLatch;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LifecycleManagerTest {
    private static Logger logger = LoggerFactory.getLogger(LifecycleManagerTest.class);
    private LifecycleManager lifecycleManager = new LifecycleManager();

    class LifecycleListener1 implements com.netflix.governator.spi.LifecycleListener {
        LifecycleListener2 ll2 = new LifecycleListener2();
        private volatile boolean started;
        private volatile boolean stopped;


        @Override
        public void onStarted() {
            logger.info("ll1 - onStarted() called");
            final CountDownLatch nestingLatch = new CountDownLatch(1);
            new Thread() {
                public void run() {
                    lifecycleManager.addListener(ll2);
                    nestingLatch.countDown();
                }
            }.start();
            try {
                nestingLatch.await();
                started = true;
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            logger.info("ll1 - onStarted() completed");
        }

        @Override
        public void onStopped(Throwable error) {
            stopped = true;
            logger.info("ll1 - onStopped() called w/error thrown: " + error);
        }

    }

    class LifecycleListener2 implements com.netflix.governator.spi.LifecycleListener {
        private volatile boolean started;
        private volatile boolean stopped;

        @Override
        public void onStarted() {
            logger.info("ll2 - onStarted() called");
            started = true;
            logger.info("ll2 - onStarted() completed");
        }

        @Override
        public void onStopped(Throwable error) {
            logger.info("ll2 - onStopped() called w/error thrown: " + error);
            stopped = true;
        }

    }

    @Test
    public void testNestedMultithreadedLifecycleListeners() {
        LifecycleListener1 ll1 = new LifecycleListener1();
        lifecycleManager.addListener(ll1);
        Assert.assertFalse(ll1.started);
        Assert.assertFalse(ll1.ll2.started);
        Assert.assertFalse(ll1.stopped);
        Assert.assertFalse(ll1.ll2.stopped);
        
        lifecycleManager.notifyStarted();
        Assert.assertTrue(ll1.started);
        Assert.assertTrue(ll1.ll2.started);
        
        lifecycleManager.notifyShutdown();
        Assert.assertTrue(ll1.stopped);
        Assert.assertTrue(ll1.ll2.stopped);
        
    }
}
