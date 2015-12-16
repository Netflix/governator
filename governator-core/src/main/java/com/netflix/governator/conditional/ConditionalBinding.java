package com.netflix.governator.conditional;

import java.util.List;

import com.google.inject.Binding;
import com.google.inject.Key;
import com.google.inject.spi.Element;

/**
 * Binding object passed to a ConditionalBindingTargetVisitor when visiting
 * Guice's bindings
 */
public interface ConditionalBinding<T> {
    /**
     * @return  Key for the main type for which there are conditional bindings
     */
    Key<T> getKey();

    /**
     * @return  All candidate elements of which one should match
     */
    List<Binding<?>> getCandidateElements();

    boolean containsElement(Element element);
}
