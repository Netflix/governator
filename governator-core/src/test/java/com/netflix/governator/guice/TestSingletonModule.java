package com.netflix.governator.guice;

import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Guice;

public class TestSingletonModule {
    public static interface Foo {
    }
    
    public static class Foo1 implements Foo {
    }
    
    public static class Foo2 implements Foo {
    }
    
    public static final class TestModule extends SingletonModule {
        @Override
        protected void configure() {
            Foo foo = new Foo1();
            bind(Foo.class).toInstance(foo);
        }
    }
    
    public static final class NoDedupTestModule extends AbstractModule {
        @Override
        protected void configure() {
            Foo foo = new Foo1();
            bind(Foo.class).toInstance(foo);
        }
    }

    @Test(expectedExceptions={CreationException.class})
    public void confirmDupExceptionBehavior() {
        Guice.createInjector(new NoDedupTestModule(), new NoDedupTestModule());
    }
    
    @Test
    public void moduleAddedToInjectorTwiceWillDedup() {
        Guice.createInjector(new TestModule(), new TestModule());
    }
    
    @Test
    public void moduleInstalledTwiceWillDedup() {
        Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                this.install(new TestModule());
                this.install(new TestModule());
            }
        });
    }
}
