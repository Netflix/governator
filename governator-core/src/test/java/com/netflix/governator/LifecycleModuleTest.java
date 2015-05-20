package com.netflix.governator;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import junit.framework.Assert;

import org.junit.Test;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.netflix.governator.guice.InjectorLifecycle;
import com.netflix.governator.guice.LifecycleModule;

public class LifecycleModuleTest {
    @Singleton
    private static class MySingleton {
        private AtomicInteger initCounter = new AtomicInteger(0);
        private AtomicInteger shutdownCounter = new AtomicInteger(0);
        
        @PostConstruct
        void init() {
            initCounter.incrementAndGet();
        }
        
        @PreDestroy
        void shutdown() {
            shutdownCounter.incrementAndGet();
        }
    }
    
    @Test
    public void testWithoutLifecycle() {
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT);
        MySingleton singleton = injector.getInstance(MySingleton.class);
        InjectorLifecycle.shutdown(injector);
        
        Assert.assertEquals(0, singleton.initCounter.get());
        Assert.assertEquals(0, singleton.shutdownCounter.get());
    }
    
    @Test
    public void testWithLifecycle() {
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, new LifecycleModule());
        MySingleton singleton = injector.getInstance(MySingleton.class);
        Assert.assertEquals(1, singleton.initCounter.get());
        InjectorLifecycle.shutdown(injector);
        Assert.assertEquals(1, singleton.shutdownCounter.get());
    }
}
