package com.netflix.governator.guice;

import com.google.inject.Module;

public class Suites {
    public static LifecycleInjectorBuilderSuite from(final Class<? extends Module> module) {
        return new LifecycleInjectorBuilderSuite() {
            @Override
            public void configure(LifecycleInjectorBuilder builder) {
                builder.withAdditionalModuleClasses(module);
            }
        };
    }
    
    public static LifecycleInjectorBuilderSuite from(final Module module) {
        return new LifecycleInjectorBuilderSuite() {
            @Override
            public void configure(LifecycleInjectorBuilder builder) {
                builder.withAdditionalModules(module);
            }
        };
    }
}
