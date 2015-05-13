package com.netflix.governator.guice.bootstrap;

import javax.inject.Inject;

import com.netflix.governator.annotations.Modules;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;

public class ModulesBootstrap implements BootstrapModule {
    private final Modules modules;
    
    @Inject
    public ModulesBootstrap(Modules modules) {
        this.modules = modules;
    }
    
    @Override
    public void configure(BootstrapBinder binder) {
        binder.include(modules.include());
        binder.exclude(modules.exclude());
    }

}
