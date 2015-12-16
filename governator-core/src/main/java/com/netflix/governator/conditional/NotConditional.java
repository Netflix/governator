package com.netflix.governator.conditional;

import com.google.inject.Injector;

/**
 * Conditional equivalent to 
 * 
 *  !conditional
 */
public class NotConditional implements Conditional {
    private final Conditional conditional;
    
    public NotConditional(Conditional conditional) {
        this.conditional = conditional;
    }
    
    @Override
    public boolean matches(Injector injector) {
        return !conditional.matches(injector);
    }
}
