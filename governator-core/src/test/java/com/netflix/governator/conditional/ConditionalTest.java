package com.netflix.governator.conditional;

import junit.framework.Assert;

import org.junit.Test;

import com.google.inject.Injector;

public class ConditionalTest {
    public static class True implements Conditional {
        @Override
        public boolean matches(Injector injector) {
            return true;
        }
    }
    
    public static class False implements Conditional {
        @Override
        public boolean matches(Injector injector) {
            return false;
        }
    }
    
    @Test
    public void testOr() {
        Assert.assertTrue(Conditionals.anyOf(new True(), new True()).matches(null));
        Assert.assertTrue(Conditionals.anyOf(new True(), new False()).matches(null));
        Assert.assertTrue(Conditionals.anyOf(new False(), new True()).matches(null));
        Assert.assertFalse(Conditionals.anyOf(new False(), new False()).matches(null));
    }
    
    @Test
    public void testAnd() {
        Assert.assertTrue(Conditionals.allOf(new True(), new True()).matches(null));
        Assert.assertFalse(Conditionals.allOf(new True(), new False()).matches(null));
        Assert.assertFalse(Conditionals.allOf(new False(), new True()).matches(null));
        Assert.assertFalse(Conditionals.allOf(new False(), new False()).matches(null));
    }
    
    @Test
    public void testNot() {
        Assert.assertFalse(Conditionals.not(new True()).matches(null));
        Assert.assertTrue(Conditionals.not(new False()).matches(null));
    }
}
