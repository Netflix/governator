package com.netflix.governator.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.netflix.governator.guice.actions.BindingReport;
import com.netflix.governator.guice.actions.CreateAllBoundSingletons;
import com.netflix.governator.guice.actions.GrapherAction;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import java.util.concurrent.atomic.AtomicInteger;

import javax.inject.Inject;
import javax.inject.Provider;

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
    
    @Rule
    public TestName testName = new TestName();
    
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
    
    @Before
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
            .withPostInjectorAction(new BindingReport(testName.getMethodName()))
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
            .withPostInjectorAction(new BindingReport(testName.getMethodName()))
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
            .withPostInjectorAction(new BindingReport(testName.getMethodName()))
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
            .withPostInjectorAction(new BindingReport(testName.getMethodName()))
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
