package com.netflix.governator.conditional;

/**
 * Contract for any conditional that may be applied to a conditional binding
 * bound via {@link ConditionalBinder}.
 * 
 * Dependencies needed by a concrete conditional should be injected using 
 * member injection.
 * 
 */
public abstract class Conditional {
    /**
     * Evaluate whether the condition is true.  evaluate() is only called once at injector
     * creation time.
     * @return True if conditional is true otherwise false
     */
    public abstract boolean evaluate();
    
    /**
     * @param conditional
     * @return Composite conditional that does a logical AND of this conditional with another
     */
    public abstract Conditional and(Conditional conditional);
    
    /**
     * @param conditional
     * @return Composite conditional that does a logical OR of this conditional with another
     */
    public abstract Conditional or(Conditional conditional);
    
    /**
     * @return Create a conditional that is a logical NOT of the provided conditional
     */
    public static Conditional not(Conditional conditional) {
        return new NotConditional(conditional);
    }
}
