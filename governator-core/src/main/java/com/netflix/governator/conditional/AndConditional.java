package com.netflix.governator.conditional;

import com.google.inject.Injector;

/**
 * Conditional equivalent to 
 * 
 *  first && second
 */
public class AndConditional extends AbstractConditional {
    private final Conditional first;
    private final Conditional second;
    
    public AndConditional(Conditional first, Conditional second) {
        this.first = first;
        this.second = second;
    }

    @Override
    public boolean matches(Injector injector) {
        return first.matches(injector) && second.matches(injector);
    }
}
