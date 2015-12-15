package com.netflix.governator.conditional;

/**
 * Conditional equivalent to 
 * 
 *  first || second
 */
public class OrConditional extends AbstractConditional {
    private final Conditional first;
    private final Conditional second;
    
    public OrConditional(AbstractConditional first, Conditional second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean evaluate() {
        return first.evaluate() || second.evaluate();
    }
}
