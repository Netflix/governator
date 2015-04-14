package com.netflix.governator.guice.bootstrap;

import javax.inject.Inject;

import com.netflix.governator.annotations.Modules;
import com.netflix.governator.guice.AbstractBootstrapModule;

public class ModulesBootstrap extends AbstractBootstrapModule {
    private final Modules modules;
    
    @Inject
    public ModulesBootstrap(Modules modules) {
        this.modules = modules;
    }
    
    @Override
    public void configure() {
        include(modules.include());
        exclude(modules.exclude());
    }

}
