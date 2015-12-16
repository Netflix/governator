package com.netflix.governator.conditional;

import java.util.ArrayList;
import java.util.List;

import com.google.inject.Injector;

/**
 * Conditional that is true if and only if all child conditionals are true
 */
public class AllOfConditional implements Conditional {
    private final List<Conditional> children;
    
    public AllOfConditional(List<Conditional> children) {
        this.children = new ArrayList<>(children);
    }

    @Override
    public boolean matches(Injector injector) {
        for (Conditional conditional : children) {
            if (!conditional.matches(injector)) {
                return false;
            }
        }
        return true;
    }
}
