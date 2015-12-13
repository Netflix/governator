package com.netflix.governator.visitors;

import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.StaticInjectionRequest;

/**
 * Predicate visitor that returns 'true' if an Element is a requestStaticInjection.
 */
public class IsStaticInjectionVisitor extends DefaultElementVisitor<Boolean> { 
    @Override
    protected Boolean visitOther(Element element) {
        return false;
    }

    @Override 
    public Boolean visit(StaticInjectionRequest element) { 
        return true;
    } 
}
