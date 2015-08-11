package com.netflix.governator.autobindmodule.good;

import javax.inject.Inject;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule; 
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.guice.LifecycleInjector;

public class TestAutoBindModuleInjection {
    public static class FooModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(String.class).annotatedWith(Names.named("foo")).toInstance("found");
        }
    }
    
    @AutoBindSingleton
    public static class MyModule extends AbstractModule {
        @Inject
        private MyModule(FooModule foo) {
        }
        
        @Override
        protected void configure() {
            bind(String.class).annotatedWith(Names.named("MyModule")).toInstance("found");
        }
    }
    
    @AutoBindSingleton
    public static class MyModule2 extends AbstractModule {
        @Inject
        private MyModule2() {
        }
        
        @Override
        protected void configure() {
            bind(String.class).annotatedWith(Names.named("MyModule2")).toInstance("found");
        }
    }
    
    @Test
    public void shouldInjectModule() {
        Injector injector = LifecycleInjector.builder().usingBasePackages("com.netflix.governator.autobindmodule")
            .build()
            .createInjector();
        
        Assert.assertEquals("found", injector.getInstance(Key.get(String.class, Names.named("MyModule"))));
        Assert.assertEquals("found", injector.getInstance(Key.get(String.class, Names.named("MyModule2"))));
        Assert.assertEquals("found", injector.getInstance(Key.get(String.class, Names.named("foo"))));
    }
}
