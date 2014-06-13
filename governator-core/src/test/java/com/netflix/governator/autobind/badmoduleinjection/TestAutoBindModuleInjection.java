package com.netflix.governator.autobind.badmoduleinjection;

import javax.inject.Inject;

import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.guice.LifecycleInjector;

public class TestAutoBindModuleInjection {
    public static class Foo {
        
    }
    
    @AutoBindSingleton
    public static class MyModule extends AbstractModule {
        
        @Inject
        MyModule(Foo foo) {
            
        }
        
        @Override
        protected void configure() {
            // TODO Auto-generated method stub
            
        }
        
    }
    
    @Test(expectedExceptions=CreationException.class)
    public void shouldFailToInjectIntoModule() {
        LifecycleInjector.builder().usingBasePackages("com.netflix.governator.autobind.badmoduleinjection")
            .build()
            .createInjector();
        
    }
}
