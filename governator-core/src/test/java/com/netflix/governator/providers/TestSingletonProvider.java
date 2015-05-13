package com.netflix.governator.providers;

import junit.framework.Assert;

import org.testng.annotations.Test;

public class TestSingletonProvider {
    public static class Foo {
        
    }
    
    public static class StringProvider extends SingletonProvider<Foo> {
        @Override
        protected Foo create() {
            return new Foo();
        }
    }
    
    @Test
    public void testSingletonBehavior() {
        StringProvider provider = new StringProvider();
        Foo foo1 = provider.get();
        Foo foo2 = provider.get();
        
        Assert.assertSame(foo1, foo2);
    }
}
