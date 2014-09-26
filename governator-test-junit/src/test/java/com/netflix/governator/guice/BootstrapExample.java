package com.netflix.governator.guice;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.netflix.governator.guice.annotations.GovernatorConfiguration;

public class BootstrapExample {
    public static interface Foo {
        
    }
    
    public static class FooImpl implements Foo {
        
    }
    
    public static class FooSuite extends AbstractModule {
        @Override
        protected void configure() {
            bind(Foo.class).to(FooImpl.class);
        }
    }
    
    @GovernatorConfiguration
    public static class Configuration extends AbstractModule {
        @Override
        protected void configure() {
            bind(String.class).toInstance("configuration");
        }
    }
    
    @Rule
    public LifecycleTester tester = new LifecycleTester(Configuration.class);
    
    @Test
    public void testFooSuiteInstalled() {
        tester.withExternalBindings(new FooSuite());
        tester.start();
        
        String foo = tester.getInstance(String.class);
        Assert.assertEquals("configuration",  foo);
        tester.getInstance(Foo.class);
    }
}
