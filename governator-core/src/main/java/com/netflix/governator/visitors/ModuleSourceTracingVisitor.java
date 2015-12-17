package com.netflix.governator.visitors;

import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.ElementSource;

/**
 * Visitor for logging the 'path' through which each binding was created
 */
public class ModuleSourceTracingVisitor  extends DefaultElementVisitor<String> { 
    @Override 
    protected String visitOther(Element element) {
        Object source = element.getSource();
        ElementSource elementSource = null;
        while (source instanceof ElementSource) {
            elementSource = (ElementSource)source;
            source = elementSource.getOriginalElementSource();
        }
        
        if (elementSource != null) {
            return elementSource.getModuleClassNames().toString();
        }

        return null;
    }
}
