package com.netflix.governator.visitors;

import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.StaticInjectionRequest;

/**
 * Predicate visitor that returns 'true' if an Element is a requestStaticInjection.
 */
public class IsNotStaticInjectionVisitor extends DefaultElementVisitor<Boolean> { 
    @Override
    protected Boolean visitOther(Element element) {
        return true;
    }

    @Override 
    public Boolean visit(StaticInjectionRequest element) { 
        return false;
    } 
}
