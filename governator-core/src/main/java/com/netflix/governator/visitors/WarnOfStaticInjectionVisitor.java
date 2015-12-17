package com.netflix.governator.visitors;

import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.StaticInjectionRequest;

/**
 * Visitor that log a warning for any use of requestStaticInjection
 */
public class WarnOfStaticInjectionVisitor extends DefaultElementVisitor<String> { 
    @Override 
    public String visit(StaticInjectionRequest element) { 
        return String.format("Static injection is fragile! Please fix %s at %s",  
          element.getType().getName(), element.getSource()); 
    } 
}
