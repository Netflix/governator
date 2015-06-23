package com.netflix.governator;

import com.google.inject.AbstractModule;

/**
 * Install this module to log a Provision report after the Injector is created.
 * 
 * @author elandau
 *
 */
public class ProvisionDebugModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(LoggingProvisionMetricsLifecycleListener.class).asEagerSingleton();
        bind(ProvisionMetrics.class).to(SimpleProvisionMetrics.class);
    }
}
