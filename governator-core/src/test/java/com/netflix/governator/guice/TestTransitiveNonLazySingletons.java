package com.netflix.governator.guice;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Singleton;

import junit.framework.Assert;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.google.inject.Stage;
import com.netflix.governator.guice.actions.BindingReport;
import com.netflix.governator.guice.actions.CreateAllBoundSingletons;

public class TestTransitiveNonLazySingletons {
    
    private String testName;
    
    @BeforeMethod
    public void handleTestMethodName(Method method)
    {
        testName = method.getName(); 
    }
    
    @Singleton
    public static class ThisShouldBeLazy {
        public static final AtomicLong counter = new AtomicLong(0);
        
        public ThisShouldBeLazy() {
            System.out.println("ThisShouldBeLazy");
            counter.incrementAndGet();
        }
    }
    
    @Singleton
    public static class ThisShouldBeEager {
        public static final AtomicLong counter = new AtomicLong(0);
        
        @Inject
        public ThisShouldBeEager(Provider<ThisShouldBeLazy> provider) {
            System.out.println("ThisShouldBeEager");
            counter.incrementAndGet();
        }
    }
    
    @BeforeMethod
    public void setup() {
        System.out.println("setup");
        ThisShouldBeLazy.counter.set(0);
        ThisShouldBeEager.counter.set(0);
    }
    
    @Test
    public void shouldNotCreateLazyTransitiveSingleton_Production_Child() {
        Injector injector = LifecycleInjector.builder()
                .withModules(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ThisShouldBeEager.class);
                    }
                })
                .withPostInjectorAction(new BindingReport(testName))
                .build()
                .createInjector();
            
            Assert.assertEquals(0,  ThisShouldBeLazy.counter.get());
            Assert.assertEquals(1,  ThisShouldBeEager.counter.get());
    }
    
    @Test
    public void shouldCreateAllSingletons_Production_NoChild() {
        Injector injector = LifecycleInjector.builder()
            .withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS)
            .withModules(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(ThisShouldBeEager.class);
                }
            })
            .withPostInjectorAction(new BindingReport(testName))
            .build()
            .createInjector();
        
        Assert.assertEquals(1,  ThisShouldBeLazy.counter.get());
        Assert.assertEquals(1,  ThisShouldBeEager.counter.get());
    }
    
    @Test
    public void shouldNotCreateLazyTransitiveSingleton_Development_NoChild() {
        Injector injector = LifecycleInjector.builder()
            .inStage(Stage.DEVELOPMENT)
            .withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS)
            .withModules(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(ThisShouldBeEager.class).asEagerSingleton();
                }
            })
            .withPostInjectorAction(new BindingReport(testName))
            .build()
            .createInjector();
        
        Assert.assertEquals(0,  ThisShouldBeLazy.counter.get());
        Assert.assertEquals(1,  ThisShouldBeEager.counter.get());
    }
    
    @Test
    public void shouldNotCreateAnyNonEagerSingletons_Development_NoChild() {
        Injector injector = LifecycleInjector.builder()
                .inStage(Stage.DEVELOPMENT)
                .withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS)
                .withModules(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ThisShouldBeEager.class);
                    }
                })
                .withPostInjectorAction(new BindingReport(testName))
                .build()
                .createInjector();
            
            Assert.assertEquals(0,  ThisShouldBeLazy.counter.get());
            Assert.assertEquals(0,  ThisShouldBeEager.counter.get());
    }
    
    @Test
    public void shouldPostCreateAllBoundNonTransitiveSingletons_Development_NoChild() {
        Injector injector = LifecycleInjector.builder()
                .inStage(Stage.DEVELOPMENT)
                .withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS)
                .withModules(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ThisShouldBeEager.class);
                    }
                })
                .withPostInjectorAction(new BindingReport(testName))
                .withPostInjectorAction(new CreateAllBoundSingletons())
                .build()
                .createInjector();
            
            Assert.assertEquals(0,  ThisShouldBeLazy.counter.get());
            Assert.assertEquals(1,  ThisShouldBeEager.counter.get());
    }
}
