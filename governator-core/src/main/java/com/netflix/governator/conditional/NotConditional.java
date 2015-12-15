package com.netflix.governator.conditional;

/**
 * Conditional equivalent to 
 * 
 *  !conditional
 */
public class NotConditional extends AbstractConditional {
    private final Conditional conditional;
    
    public NotConditional(Conditional conditional) {
        this.conditional = conditional;
    }
    
    @Override
    public boolean evaluate() {
        return !conditional.evaluate();
    }
}
