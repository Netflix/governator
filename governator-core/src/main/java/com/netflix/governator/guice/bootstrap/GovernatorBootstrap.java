package com.netflix.governator.guice.bootstrap;

import com.google.inject.Inject;
import com.google.inject.ProvisionException;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.guice.LifecycleInjectorBuilderSuite;
import com.netflix.governator.guice.ModuleTransformer;
import com.netflix.governator.guice.PostInjectorAction;
import com.netflix.governator.guice.annotations.GovernatorConfiguration;

/**
 * Implementation for the @GovernatorConfiguration main bootstrap class annotation
 */
public class GovernatorBootstrap implements LifecycleInjectorBuilderSuite{

    private final GovernatorConfiguration config;
    
    @Inject
    public GovernatorBootstrap(GovernatorConfiguration config) {
        this.config = config;
    }
    
    @Override
    public void configure(LifecycleInjectorBuilder builder) {
        if (config.enableAutoBindSingleton() == false)
            builder.ignoringAllAutoBindClasses();
        builder.inStage(config.stage());
        builder.withMode(config.mode());
        
        for (Class<? extends PostInjectorAction> action : config.actions()) {
            try {
                builder.withPostInjectorAction(action.newInstance());
            } catch (Exception e) {
                throw new ProvisionException("Error creating postInjectorAction '" + action.getName() + "'", e);
            }
        }
        
        for (Class<? extends ModuleTransformer> transformer : config.transformers()) {
            try {
                builder.withModuleTransformer(transformer.newInstance());
            } catch (Exception e) {
                throw new ProvisionException("Error creating postInjectorAction '" + transformer.getName() + "'", e);
            }
        }
    }

}
