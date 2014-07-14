package com.netflix.governator.guice.transformer;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.netflix.governator.guice.LifecycleInjector;

public class OverrideAllDuplicateBindingsTest {
    public static interface Foo {
        
    }
    public static class Foo1 implements Foo {
        
    }
    public static class Foo2 implements Foo {
        
    }
    
    public static class MyModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(Foo.class).to(Foo1.class);
        }
    }
    
    public static class MyOverrideModule extends AbstractModule {
        @Override
        protected void configure() {
            bind(Foo.class).to(Foo2.class);
        }
    }
    
    @Test
    public void testShouldFailOnDuplicate() {
        try {
            LifecycleInjector.builder()
                .withModuleClasses(MyModule.class, MyOverrideModule.class)
                .build()
                .createInjector();
            Assert.fail("Should have failed with duplicate binding");
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    @Test
    public void testShouldInstallDuplicate() {
        Injector injector = LifecycleInjector.builder()
            .withModuleTransformer(new OverrideAllDuplicateBindings())
            .withModuleClasses(MyModule.class, MyOverrideModule.class)
            .build()
            .createInjector();
        
        Foo foo = injector.getInstance(Foo.class);
        Assert.assertTrue(foo.getClass().equals(Foo2.class));
    }
}
