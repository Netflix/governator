package com.netflix.governator.autobindmodule.good;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.guice.LifecycleInjector;

public class TestAutoBindModuleInjection {
    public static class FooModule extends AbstractModule {
        @Override
        protected void configure() {
        }
    }
    
    @AutoBindSingleton
    public static class MyModule extends AbstractModule {
        @Inject
        MyModule(FooModule foo) {
        }
        
        @Override
        protected void configure() {
        }
    }
    
    @Test
    public void shouldInjectModule() {
        LifecycleInjector.builder().usingBasePackages(TestAutoBindModuleInjection.class.getPackage().getName())
            .build()
            .createInjector();
        
    }
}
