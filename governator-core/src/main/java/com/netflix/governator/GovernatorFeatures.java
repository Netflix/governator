package com.netflix.governator;


/**
 * Core Governator features.  Features are configured/enabled on {@link Governator}
 */
public final class GovernatorFeatures  {
    /**
     * When disable the Governator process will continue running even if there is a catastrophic 
     * startup failure.  This allows the admin page to stay up so that the process may be 
     * debugged more easily. 
     */
    public static final GovernatorFeature<Boolean> SHUTDOWN_ON_ERROR = GovernatorFeature.create("Governator.features.shutdownOnError", true);
    
    /**
     * Auto discover AutoBinders using the ServiceLoader
     */
    public static final GovernatorFeature<Boolean> DISCOVER_AUTO_BINDERS = GovernatorFeature.create("Governator.features.discoverAutoBinders", true);
    
    /**
     * Enables strict validation of @PostConstruct / @PreDestroy annotations at runtime; default is false
     */
    public static final GovernatorFeature<Boolean> STRICT_JSR250_VALIDATION = GovernatorFeature.create("Governator.features.strictJsr250Validation", false);
    
}
