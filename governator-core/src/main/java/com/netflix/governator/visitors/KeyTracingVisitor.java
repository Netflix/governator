package com.netflix.governator.visitors;

import com.google.inject.Binding;
import com.google.inject.spi.DefaultElementVisitor;
import com.netflix.governator.InjectorBuilder;

/**
 * Visitor for logging only the Key for each {@code Element} binding
 * 
 * To use with {@link InjectorBuilder}
 * 
 * <code>
 * InjectorBuilder
 *      .fromModule(new MyApplicationModule)
 *      .forEachElement(new BindingTracingVisitor())
 *      .createInjector();
 * </code>
 */
public class KeyTracingVisitor extends DefaultElementVisitor<String> {
    @Override
    public <T> String visit(Binding<T> binding) {
        return binding.getKey().toString();
    }
}
