package com.netflix.governator.visitors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binding;
import com.google.inject.spi.DefaultElementVisitor;
import com.netflix.governator.ModuleBuilder;

/**
 * Visitor for logging only the Key for each {@code Element} binding
 * 
 * To use with {@link ModuleBuilder}
 * 
 * <code>
 * ModuleBuilder
 *      .fromModule(new MyApplicationModule)
 *      .forEachElement(new BindingTracingVisitor())
 *      .createInjector();
 * </code>
 */
public class KeyTracingVisitor extends DefaultElementVisitor<Void> {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleBuilder.class);
    
    private final String prefix;
    
    public KeyTracingVisitor() {
        this("");
    }
    
    public KeyTracingVisitor(String prefix) {
        this.prefix = prefix == null ? "" : prefix + " : ";
    }
    
    @Override
    public <T> Void visit(Binding<T> binding) {
        LOG.info(prefix + binding.getKey().toString());
        return null;
    }
}
