package com.netflix.governator.conditional;

import junit.framework.Assert;

import org.junit.Test;

public class ConditionalTest {
    public static class True extends AbstractConditional {
        @Override
        public boolean evaluate() {
            return true;
        }
    }
    
    public static class False extends AbstractConditional {
        @Override
        public boolean evaluate() {
            return false;
        }
    }
    
    @Test
    public void testOr() {
        Assert.assertTrue(new True().or(new True()).evaluate());
        Assert.assertTrue(new True().or(new False()).evaluate());
        Assert.assertTrue(new False().or(new True()).evaluate());
        Assert.assertFalse(new False().or(new False()).evaluate());
    }
    
    @Test
    public void testAnd() {
        Assert.assertTrue(new True().and(new True()).evaluate());
        Assert.assertFalse(new True().and(new False()).evaluate());
        Assert.assertFalse(new False().and(new True()).evaluate());
        Assert.assertFalse(new False().and(new False()).evaluate());
    }
    
    @Test
    public void testNot() {
        Assert.assertFalse(Conditional.not(new True()).evaluate());
        Assert.assertTrue(Conditional.not(new False()).evaluate());
    }
}
