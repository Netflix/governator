package com.netflix.governator;

import java.util.concurrent.atomic.AtomicBoolean;

import junit.framework.Assert;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.netflix.governator.spi.LifecycleListener;

public class GovernatorTest {
    @Test
    public void testSimpleExecution() {
        final AtomicBoolean isStopped = new AtomicBoolean();
        final AtomicBoolean isStarted = new AtomicBoolean();
        LifecycleInjector injector = new Governator()
            .addModules(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(String.class).toInstance("foo");
                }
            })
            .run(new LifecycleListener() {
                @Override
                public void onStarted() {
                    isStarted.set(true);
                }

                @Override
                public void onStopped(Throwable error) {
                    isStopped.set(true);
                }
                
                @Override
                public String toString() {
                    return "UnitTest[]";
                }
            });
        
        try {
            Assert.assertTrue(isStarted.get());
            Assert.assertFalse(isStopped.get());
            Assert.assertEquals("foo", injector.getInstance(String.class));
        }
        finally {
            injector.shutdown();
        }
        Assert.assertTrue(isStopped.get());
    }
}
