package com.netflix.governator.guice.bootstrap;

import com.google.inject.Inject;
import com.google.inject.ProvisionException;
import com.netflix.governator.guice.AbstractBootstrapModule;
import com.netflix.governator.guice.ModuleTransformer;
import com.netflix.governator.guice.PostInjectorAction;
import com.netflix.governator.guice.annotations.GovernatorConfiguration;

/**
 * Implementation for the @GovernatorConfiguration main bootstrap class annotation
 */
public class GovernatorBootstrap extends AbstractBootstrapModule {

    private final GovernatorConfiguration config;
    
    @Inject
    public GovernatorBootstrap(GovernatorConfiguration config) {
        this.config = config;
    }
    
    @Override
    public void configure() {
        if (!config.enableAutoBindSingleton()) {
            disableAutoBinding();
        }
        inStage(config.stage());
        inMode(config.mode());
        
        for (Class<? extends PostInjectorAction> action : config.actions()) {
            try {
                bindPostInjectorAction().to(action);
            } catch (Exception e) {
                throw new ProvisionException("Error creating postInjectorAction '" + action.getName() + "'", e);
            }
        }
        
        for (Class<? extends ModuleTransformer> transformer : config.transformers()) {
            try {
                bindModuleTransformer().to(transformer);
            } catch (Exception e) {
                throw new ProvisionException("Error creating postInjectorAction '" + transformer.getName() + "'", e);
            }
        }    
    }
}
