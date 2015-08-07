package com.netflix.governator;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import junit.framework.Assert;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Stage;
import com.netflix.governator.annotations.Configuration;

@Test(singleThreaded=true)
public class LifecycleModuleTest {
    @Singleton
    private static class MyStateInjectableClass {
        @Inject
        public static void staticInject(MySingleton singleton) {
            System.out.println("*****Static injection");
        }
    }
    
    @Singleton
    private static class MySingleton {
        private static AtomicInteger initCounter = new AtomicInteger(0);
        private static AtomicLong initTime = new AtomicLong(0);
        private static AtomicInteger shutdownCounter = new AtomicInteger(0);
        private static AtomicLong shutdownTime = new AtomicLong(0);
        
        @Configuration("foo")
        String field1;
        
        @Configuration("goo")
        public void setGoo(String goo) {
            
        }
        
        @Inject
        public MySingleton() {
            System.out.println("*****Injecting MySingleton");
        }
        
        @PostConstruct
        void init() {
            System.out.println("*****Post constructing");
            initCounter.incrementAndGet();
            initTime.set(System.nanoTime());
        }
        
        @PreDestroy
        void shutdown() {
            shutdownCounter.incrementAndGet();
            shutdownTime.set(System.nanoTime());
        }
    }
    
    @Singleton
    private static class FailingSingleton {
        @Inject
        public FailingSingleton() {
            throw new RuntimeException("Failing singleton");
        }
    }
    
    @Singleton
    private static class SingletonWithDependency {
        private static AtomicInteger initCounter = new AtomicInteger(0);
        private static AtomicLong initTime = new AtomicLong(0);
        private static AtomicInteger shutdownCounter = new AtomicInteger(0);
        private static AtomicLong shutdownTime = new AtomicLong(0);
        
        @Inject
        public SingletonWithDependency(MySingleton mySingleton) {
            System.out.println("*****Injecting SingletonWithDependency");
        }
        
        @PostConstruct
        void init() {
            System.out.println("*****Post constructing SingletonWithDependency");
            initCounter.incrementAndGet();
            initTime.set(System.nanoTime());
        }
        
        @PreDestroy
        void shutdown() {
            shutdownCounter.incrementAndGet();
            shutdownTime.set(System.nanoTime());
        }
    }
    
    @BeforeMethod
    public void before() {
        MySingleton.initCounter.set(0);
        MySingleton.shutdownCounter.set(0);
    }
    
    @Test
    public void testWithoutLifecycle() {
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT);
        MySingleton singleton = injector.getInstance(MySingleton.class);
        
        Assert.assertEquals(0, singleton.initCounter.get());
        Assert.assertEquals(0, singleton.shutdownCounter.get());
    }
    
    @Test
    public void testWithLifecycle() {
        LifecycleInjector injector = Governator.createInjector(
                Stage.DEVELOPMENT);
        MySingleton singleton = injector.getInstance(MySingleton.class);
        Assert.assertEquals(1, singleton.initCounter.get());
        Assert.assertEquals(0, singleton.shutdownCounter.get());
        injector.shutdown();
        Assert.assertEquals(1, singleton.shutdownCounter.get());
    }

    @Test
    public void testOrderWithLifecycle() {
        LifecycleInjector injector = Governator.createInjector(
                Stage.DEVELOPMENT);
        SingletonWithDependency singleton = injector.getInstance(SingletonWithDependency.class);
        Assert.assertEquals(1, singleton.initCounter.get());
        Assert.assertEquals(1, MySingleton.initCounter.get());
        Assert.assertTrue("MySingleton was constructed before SingletonWithDependency",
                MySingleton.initTime.get() < singleton.initTime.get());
        Assert.assertEquals(0, singleton.shutdownCounter.get());
        Assert.assertEquals(0, MySingleton.shutdownCounter.get());
        injector.shutdown();
        Assert.assertEquals(1, singleton.shutdownCounter.get());
        Assert.assertEquals(1, MySingleton.shutdownCounter.get());
        Assert.assertTrue("SingletonWithDependency was destroyed before MySingleton",
                MySingleton.shutdownTime.get() > singleton.shutdownTime.get());
    }
    
    @Test
    public void testWithExternalLifecycleManager() {
        try {
            Governator.createInjector(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(MySingleton.class).asEagerSingleton();
                    bind(FailingSingleton.class).asEagerSingleton();
                }
            });
            Assert.fail("Should have failed to create injector");
        }
        catch (Exception e) {
            Assert.assertEquals(1, MySingleton.initCounter.get());
            Assert.assertEquals(1, MySingleton.shutdownCounter.get());
        }
    }

    @Test
    public void testProvidesAnnotation() {
        Assert.assertEquals(0, MySingleton.initCounter.get());
        Assert.assertEquals(0, MySingleton.shutdownCounter.get());
        
        LifecycleInjector injector = Governator.createInjector(new DefaultModule() {
            @Provides
            @Singleton
            MySingleton createSingleton() {
                System.out.println("***** Called");
                return new MySingleton();
            }
        });
        Assert.assertEquals(1, MySingleton.initCounter.get());
        injector.shutdown();
        Assert.assertEquals(1, MySingleton.shutdownCounter.get());
    }
}
