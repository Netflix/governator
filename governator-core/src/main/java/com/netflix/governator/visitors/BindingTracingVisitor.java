package com.netflix.governator.visitors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binding;
import com.google.inject.spi.DefaultElementVisitor;
import com.netflix.governator.ModuleBuilder;

/**
 * Visitor for logging the entire binding information for each Element
 * 
 * To use with {@link ModuleBuilder},
 * 
 * <code>
 * ModuleBuilder
 *      .withModules(new MyApplicationModule)
 *      .forEachElement(new BindingTracingVisitor())
 *      .createInjector();
 * </code>
 */
public class BindingTracingVisitor extends DefaultElementVisitor<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleBuilder.class);
    
    private final String prefix;
    
    public BindingTracingVisitor() {
        this("");
    }
    
    public BindingTracingVisitor(String prefix) {
        this.prefix = prefix == null ? "" : prefix + " : ";
    }
    
    @Override
    public <T> Void visit(Binding<T> binding) {
        LOG.info(prefix + binding);
        return null;
    }
}
