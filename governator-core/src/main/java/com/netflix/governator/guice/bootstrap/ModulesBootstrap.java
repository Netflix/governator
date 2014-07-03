package com.netflix.governator.guice.bootstrap;

import javax.inject.Inject;

import com.netflix.governator.annotations.Modules;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.guice.LifecycleInjectorBuilderSuite;

public class ModulesBootstrap implements LifecycleInjectorBuilderSuite {
    private final Modules modules;
    
    @Inject
    public ModulesBootstrap(Modules modules) {
        this.modules = modules;
    }
    
    @Override
    public void configure(LifecycleInjectorBuilder builder) {
        builder.withAdditionalModuleClasses(modules.include())
               .withoutModuleClasses(modules.exclude());
        
        // TODO: overrides
        
    }

}
