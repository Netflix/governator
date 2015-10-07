package com.netflix.governator;

/**
 * Core governator features.  Features are configured/enabled on {@link GovernatorConfiguration}
 * 
 * @author elandau
 * @deprecated Functionality moved https://github.com/Netflix/karyon/tree/3.x
 */
@Deprecated
public enum GovernatorFeatures implements GovernatorFeature {
    /**
     * When disabled, if the injector created using Governator.createInjector() fails the resulting
     * application will not shutdown and @PreDestroy methods will not be invoked.  This is useful
     * for debugging an application that failed to start by allowing admin servers to continue
     * running.
     */
    SHUTDOWN_ON_ERROR(true),
    ;

    private final boolean enabled;
    
    GovernatorFeatures(boolean enabled) {
        this.enabled = enabled;
    }
    
    @Override
    public boolean isEnabledByDefault() {
        return enabled;
    }

}
