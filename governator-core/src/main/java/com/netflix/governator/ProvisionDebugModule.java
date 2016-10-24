package com.netflix.governator;

import javax.inject.Inject;

/**
 * Install this module to log a Provision report after the Injector is created.
 */
public final class ProvisionDebugModule extends SingletonModule {
    @Inject
    private static void initialize(LoggingProvisionMetricsLifecycleListener listener) {
    }
    
    @Override
    protected void configure() {
        // We do a static injection here to make sure the listener gets registered early.  Otherwise,
        // if the injector fails before it's instantiated no logging will be done
        binder().requestStaticInjection(ProvisionDebugModule.class);
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
