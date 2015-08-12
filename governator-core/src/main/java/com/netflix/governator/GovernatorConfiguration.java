package com.netflix.governator;

import java.util.List;
import java.util.Set;

import com.google.inject.Module;
import com.google.inject.Stage;
import com.netflix.governator.auto.ModuleListProvider;

/**
 * Configuration contract needed to bootstrap a Governator based application.
 * 
 * @author elandau
 *
 */
public interface GovernatorConfiguration {
    /**
     * Return a list of bootstrap modules that will be loaded prior the main injector
     * being created.  Bootstrap modules should be reserved for things like configuration
     * loading that must be done before any module conditionals may be evaluated. 
     * Applications should normally use getModules()
     */
    List<Module> getBootstrapModules();
    
    /**
     * Return a list of ModuleListProvider's through which modules may be auto-loaded.
     */
    List<ModuleListProvider> getModuleListProviders();
    
    /**
     * Return a list of active profiles for the injector.  These profiles are used when
     * processing @ConditionalOnProfile annotations
     */
    Set<String> getProfiles();

    /**
     * Return the Guice injector stage.  The recommended default is Stage.DEVELOPMENT
     * otherwise all singletons are eager, including lazy injection using Provider<T> 
     */
    Stage getStage();
    
    /**
     * Determine if a core governator feature has been enabled.  See {@link GovernatorFeatures}
     * for available features.
     */
    boolean isEnabled(GovernatorFeature feature);
}
