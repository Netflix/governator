package com.netflix.governator;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Stage;
import com.netflix.governator.guice.ModulesEx;
import com.netflix.governator.guice.annotations.Bootstrap;

public class BootstrapTest {
    
    public static interface Foo {
    }

    public static class Foo1 implements Foo {
    }
    
    public static class Foo2 implements Foo {
    }
    
    @Documented
    @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    @Bootstrap(module=FooBootstrap.class)
    public static @interface FooConfiguration {
        Class<? extends Foo> foo() default Foo1.class;
    }
    
    public static class FooBootstrap extends AbstractModule {
        private FooConfiguration config;
        public FooBootstrap(FooConfiguration config) {
            this.config = config;
        }
        
        @Override
        protected void configure() {
            bind(Foo.class).to(config.foo());
        }
    }

    @FooConfiguration
    public static class MyApplication extends AbstractModule {
        @Override
        protected void configure() {
        }
    }
    
    @FooConfiguration
    public static class MyApplicationWithOverride extends AbstractModule {
        @Override
        protected void configure() {
            bind(Foo.class).to(Foo2.class);
        }
    }
    
    @Test
    public void testWithoutOverride() {
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, ModulesEx.fromClass(MyApplication.class));
        Assert.assertEquals(Foo1.class, injector.getInstance(Foo.class).getClass());
    }

    @Test(expectedExceptions=CreationException.class)
    public void testDuplicateWithoutOverride() {
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, ModulesEx.fromClass(MyApplicationWithOverride.class, false));
        Assert.fail("Should have failed with duplicate binding exception");
    }

    @Test
    public void testDuplicateWithOverride() {
        Injector injector = Guice.createInjector(Stage.DEVELOPMENT, ModulesEx.fromClass(MyApplicationWithOverride.class));
        Assert.assertEquals(Foo2.class, injector.getInstance(Foo.class).getClass());
    }

}
