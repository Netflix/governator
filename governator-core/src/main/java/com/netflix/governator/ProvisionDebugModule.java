package com.netflix.governator;

import javax.inject.Inject;

/**
 * Install this module to log a Provision report after the Injector is created.
 */
public final class ProvisionDebugModule extends SingletonModule {
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
    
    @Override
    public boolean equals(Object obj) {
        return getClass().equals(obj.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "ProvisionDebugModule[]";
    }

}
