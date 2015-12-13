package com.netflix.governator.visitors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binding;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.ElementSource;

/**
 * Visitor for logging the 'path' through which each binding was created
 */
public class ModuleSourceTracingVisitor  extends DefaultElementVisitor<Void> { 
    private static Logger LOG = LoggerFactory.getLogger(ModuleSourceTracingVisitor.class);
    
    @Override 
    public <T> Void visit(Binding<T> binding) {
        Object source = binding.getSource();
        ElementSource elementSource = null;
        while (source != null && source instanceof ElementSource) {
            elementSource = (ElementSource)source;
            source = elementSource.getOriginalElementSource();
        }
        
        if (elementSource != null) {
            LOG.info(elementSource.getModuleClassNames().toString());
        }

        return visitOther(binding);
    }
}
