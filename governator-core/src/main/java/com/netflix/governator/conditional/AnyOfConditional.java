package com.netflix.governator.conditional;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Injector;

/**
 * Conditional that is true if at least one of the child conditionals
 * is true  
 */
public class AnyOfConditional implements Conditional {
   private final List<Conditional> children;
    
    public AnyOfConditional(List<Conditional> children) {
        this.children = new ArrayList<>(children);
    }

    @Override
    public boolean matches(Injector injector) {
        for (Conditional conditional : children) {
            if (conditional.matches(injector)) {
                return true;
            }
        }
        return false;
    }
}
