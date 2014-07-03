package com.netflix.governator.guice;

import com.google.inject.AbstractModule;

public abstract class SimpleLifecycleInjectorBuilderSuite extends AbstractModule implements LifecycleInjectorBuilderSuite {
    @Override
    public void configure(LifecycleInjectorBuilder builder) {
        builder.withAdditionalModules(this);
    }
}
