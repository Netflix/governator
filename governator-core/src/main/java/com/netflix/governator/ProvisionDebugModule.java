package com.netflix.governator;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;

/**
 * Install this module to log a Provision report after the Injector is created.
 * 
 * @author elandau
 *
 */
public class ProvisionDebugModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), LifecycleListener.class).addBinding().to(LoggingProvisionMetricsLifecycleListener.class);
        bind(ProvisionMetrics.class).to(SimpleProvisionMetrics.class);

    }
}
