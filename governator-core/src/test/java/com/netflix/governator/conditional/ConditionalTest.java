package com.netflix.governator.conditional;

import junit.framework.Assert;

import org.junit.Test;

import com.google.inject.Injector;

public class ConditionalTest {
    public static class True extends AbstractConditional {
        @Override
        public boolean matches(Injector injector) {
            return true;
        }
    }
    
    public static class False extends AbstractConditional {
        @Override
        public boolean matches(Injector injector) {
            return false;
        }
    }
    
    @Test
    public void testOr() {
        Assert.assertTrue(new True().or(new True()).matches(null));
        Assert.assertTrue(new True().or(new False()).matches(null));
        Assert.assertTrue(new False().or(new True()).matches(null));
        Assert.assertFalse(new False().or(new False()).matches(null));
    }
    
    @Test
    public void testAnd() {
        Assert.assertTrue(new True().and(new True()).matches(null));
        Assert.assertFalse(new True().and(new False()).matches(null));
        Assert.assertFalse(new False().and(new True()).matches(null));
        Assert.assertFalse(new False().and(new False()).matches(null));
    }
    
    @Test
    public void testNot() {
        Assert.assertFalse(Conditional.not(new True()).matches(null));
        Assert.assertTrue(Conditional.not(new False()).matches(null));
    }
}
