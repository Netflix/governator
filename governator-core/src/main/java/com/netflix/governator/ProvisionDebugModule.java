package com.netflix.governator;

import javax.inject.Inject;

/**
 * Install this module to log a Provision report after the Injector is created.
 * 
 * @deprecated Moved to karyon
 */
@Deprecated
public class ProvisionDebugModule extends DefaultModule {
    public static class StaticInitializer {
        @Inject
        public static LoggingProvisionMetricsLifecycleListener listener;
    }
    
    @Override
    protected void configure() {
        binder().requestStaticInjection(StaticInitializer.class);
        
        bind(LoggingProvisionMetricsLifecycleListener.class).asEagerSingleton();
        bind(ProvisionMetrics.class).to(SimpleProvisionMetrics.class);
    }
}
