package com.netflix.governator.conditional;

import java.util.Arrays;

import com.google.common.base.Preconditions;

/**
 * Utility class for creating conditional
 */
public abstract class Conditionals {
    private Conditionals() {
    }
    
    /**
     * Create a conditional that matches to true if and only if all of the child conditionals
     * are true
     * 
     * @param conditional
     * @return
     */
    public static Conditional allOf(Conditional... conditional) {
        Preconditions.checkNotNull(conditional, "Cannot have a conditional allOf(null)");
        return new AllOfConditional(Arrays.asList(conditional));
    }
    
    /**
     * Create a conditional that matches to true if at least one of the child conditionals is
     * true
     * @param conditional
     * @return
     */
    public static Conditional anyOf(Conditional... conditional) {
        Preconditions.checkNotNull(conditional, "Cannot have a conditional anyOf(null)");
        return new AnyOfConditional(Arrays.asList(conditional));
    }
    
    /**
     * @return Create a conditional that is a logical NOT of the provided conditional
     */
    public static Conditional not(Conditional conditional) {
        return new NotConditional(conditional);
    }

}
