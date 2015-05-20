package com.netflix.governator;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import junit.framework.Assert;

import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.netflix.governator.guice.InjectorLifecycle;
import com.netflix.governator.guice.LifecycleModule;
import com.netflix.governator.guice.ModulesEx;

public class LifecycleModuleTest {
    @Singleton
    private static class MySingleton {
        private static AtomicInteger initCounter = new AtomicInteger(0);
        private static AtomicInteger shutdownCounter = new AtomicInteger(0);
        
        @PostConstruct
        void init() {
            initCounter.incrementAndGet();
        }
        
        @PreDestroy
        void shutdown() {
            shutdownCounter.incrementAndGet();
        }
    }
    
    @Singleton
    private static class FailingSingleton {
        @Inject
        public FailingSingleton() {
            throw new RuntimeException("Failing singleton");
        }
    }
    
    @BeforeTest
    public void before() {
        MySingleton.initCounter.set(0);
        MySingleton.shutdownCounter.set(0);
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
    
    @Test
    public void testWithExternalLifecycleManager() {
        final LifecycleManager manager = new LifecycleManager();
        try {
            Guice.createInjector(ModulesEx.combineAndOverride(new LifecycleModule(), new AbstractModule() {
                @Override
                protected void configure() {
                    bind(LifecycleManager.class).toInstance(manager);
                    bind(MySingleton.class).asEagerSingleton();
                    bind(FailingSingleton.class).asEagerSingleton();
                }
            }));
            Assert.fail("Should have failed to create injector");
        }
        catch (Exception e) {
            Assert.assertEquals(1, MySingleton.initCounter.get());
            Assert.assertEquals(0, MySingleton.shutdownCounter.get());
            manager.shutdown();
            Assert.assertEquals(1, MySingleton.shutdownCounter.get());
        }
    }

}
