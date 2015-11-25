package com.netflix.governator;

import com.google.inject.Key;

/**
 * Default NoOp implementation of ProvisionMetrics.
 * 
 * @deprecated Moved to karyon
 */
@Deprecated
public class NullProvisionMetrics implements ProvisionMetrics {
    @Override
    public void push(Key<?> key) {
    }

    @Override
    public void pop() {
    }

    @Override
    public void accept(Visitor visitor) {
    }
}
