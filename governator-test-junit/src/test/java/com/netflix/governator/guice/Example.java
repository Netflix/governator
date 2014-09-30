package com.netflix.governator.guice;

import org.junit.Rule;
import org.junit.Test;

import com.google.inject.AbstractModule;

public class Example {
    public static interface Foo {
        
    }
    
    public static class FooImpl implements Foo {
    
    }
    
    public static class FooSuite implements BootstrapModule {
        @Override
        public void configure(BootstrapBinder binder) {
            binder.include(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(Foo.class).to(FooImpl.class);
                }
            });
        }
    }
    
    @Rule
    public LifecycleTester tester = new LifecycleTester(new FooSuite());
    
    @Test
    public void testFooSuiteInstalled() {
        tester.start();
        
        Foo foo = tester.getInstance(Foo.class);
    }
}
