package com.netflix.governator.conditional;

import com.google.inject.spi.BindingTargetVisitor;

public interface ConditionalBindingsTargetVisitor<T, V> extends BindingTargetVisitor<T, V> {
    V visit(ConditionalBinding<? extends T> conditionalBinding);
}
