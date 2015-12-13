package com.netflix.governator.visitors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.StaticInjectionRequest;

/**
 * Visitor that log a warning for any use of requestStaticInjection
 */
public class WarnOfStaticInjectionVisitor extends DefaultElementVisitor<Void> { 
    private static Logger LOG = LoggerFactory.getLogger(WarnOfStaticInjectionVisitor.class);
    
    @Override 
    public Void visit(StaticInjectionRequest element) { 
      LOG.info("Static injection is fragile! Please fix {} at {}",  
          element.getType().getName(),  element.getSource()); 
      return null; 
    } 
}
