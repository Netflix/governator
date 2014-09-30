package com.netflix.governator.guice.bootstrap;

import com.google.inject.Inject;
import com.google.inject.ProvisionException;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.ModuleTransformer;
import com.netflix.governator.guice.PostInjectorAction;
import com.netflix.governator.guice.annotations.GovernatorConfiguration;

/**
 * Implementation for the @GovernatorConfiguration main bootstrap class annotation
 */
public class GovernatorBootstrap implements BootstrapModule {

    private final GovernatorConfiguration config;
    
    @Inject
    public GovernatorBootstrap(GovernatorConfiguration config) {
        this.config = config;
    }
    
    @Override
    public void configure(BootstrapBinder binder) {
        if (config.enableAutoBindSingleton() == false)
            binder.disableAutoBinding();
        binder.inStage(config.stage());
        binder.inMode(config.mode());
        
        for (Class<? extends PostInjectorAction> action : config.actions()) {
            try {
                binder.bindPostInjectorAction().to(action);
            } catch (Exception e) {
                throw new ProvisionException("Error creating postInjectorAction '" + action.getName() + "'", e);
            }
        }
        
        for (Class<? extends ModuleTransformer> transformer : config.transformers()) {
            try {
                binder.bindModuleTransformer().to(transformer);
            } catch (Exception e) {
                throw new ProvisionException("Error creating postInjectorAction '" + transformer.getName() + "'", e);
            }
        }    
    }
}
