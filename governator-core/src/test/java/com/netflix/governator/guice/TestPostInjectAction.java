package com.netflix.governator.guice;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Provider;

import junit.framework.Assert;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.netflix.governator.guice.actions.BindingReport;
import com.netflix.governator.guice.actions.CreateAllBoundSingletons;
import com.netflix.governator.guice.actions.GrapherAction;

public class TestPostInjectAction {
    @Test
    public void testPostInjectReport() {
        GrapherAction action = new GrapherAction();
        LifecycleInjector.builder()
            .withPostInjectorAction(action)
        .build()
        .createInjector();
        
        Assert.assertNotNull(action.getText());
    }
    
    public static interface Foo {
        
    }
    
    private String testName;
    
    @BeforeMethod
    public void handleTestMethodName(Method method)
    {
        testName = method.getName(); 
    }
    
    @Singleton
    public static class Transitive {
        private static AtomicInteger counter = new AtomicInteger();
        
        public Transitive() {
            counter.incrementAndGet();
        }
    }
    
    @Singleton
    public static class FooImpl implements Foo {
        private static AtomicInteger counter = new AtomicInteger();
        
        @Inject
        public FooImpl(Provider<Transitive> transitive) {
            counter.incrementAndGet();
        }
    }
    
    public static class FooNotAnnotated implements Foo {
        private static AtomicInteger counter = new AtomicInteger();
        
        @Inject
        public FooNotAnnotated(Provider<Transitive> transitive) {
            counter.incrementAndGet();
        }
    }
    
    @BeforeMethod
    public void before() {
        FooImpl.counter.set(0);
        FooNotAnnotated.counter.set(0);
        Transitive.counter.set(0);
    }
    
    @Test
    public void testClassSingleton() {
        LifecycleInjector.builder()
            .inStage(Stage.DEVELOPMENT)
            .withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS)
            .withPostInjectorAction(new BindingReport(testName))
            .withPostInjectorAction(new CreateAllBoundSingletons())
            .withModules(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(FooImpl.class);
                }
            })
        .build()
        .createInjector();
        
        Assert.assertEquals(1, FooImpl.counter.get());
        Assert.assertEquals(0, Transitive.counter.get());
    }
    
    @Test
    public void testInterfaceSingleton() {
        LifecycleInjector.builder()
            .withPostInjectorAction(new BindingReport(testName))
            .withPostInjectorAction(new CreateAllBoundSingletons())
            .inStage(Stage.DEVELOPMENT)
            .withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS)
            .withModules(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Foo.class).to(FooImpl.class);
                }
            })
        .build()
        .createInjector();
        
        Assert.assertEquals(1, FooImpl.counter.get());
        Assert.assertEquals(0, Transitive.counter.get());
    }
    
    @Test
    public void testInterfaceSingletonProductionStage() {
        LifecycleInjector.builder()
            .withPostInjectorAction(new BindingReport(testName))
            .withPostInjectorAction(new CreateAllBoundSingletons())
            .withModules(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Foo.class).to(FooImpl.class);
                }
            })
        .build()
        .createInjector();
        
        Assert.assertEquals(1, FooImpl.counter.get());
        Assert.assertEquals(0, Transitive.counter.get());
    }
    
    @Test
    public void testScopedSingleton() {
        LifecycleInjector.builder()
            .withPostInjectorAction(new BindingReport(testName))
            .withPostInjectorAction(new CreateAllBoundSingletons())
            .inStage(Stage.DEVELOPMENT)
            .withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS)
            .withModules(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Foo.class).to(FooNotAnnotated.class).in(Scopes.SINGLETON);
                }
            })
        .build()
        .createInjector();
        
        Assert.assertEquals(1, FooNotAnnotated.counter.get());
        Assert.assertEquals(0, Transitive.counter.get());
    }
    
    

}
