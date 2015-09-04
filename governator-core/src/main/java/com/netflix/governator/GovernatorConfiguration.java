package com.netflix.governator;

import java.util.List;
import java.util.Set;

import com.google.inject.Module;
import com.google.inject.Stage;
import com.netflix.governator.auto.ModuleListProvider;
import com.netflix.governator.auto.PropertySource;

/**
 * Configuration contract needed to bootstrap a Governator based application.
 * 
 * @author elandau
 *
 */
public interface GovernatorConfiguration {
    /**
     * Return the list of core application modules to be used
     */
    List<Module> getModules();
    
    /**
     * Return a list of override modules to be used as the final override for any bindings
     * specified in modules returned by getModules() and getBootstrapModules().  Override
     * modules are useful when an application has to resolve a binding conflict or when
     * testing.  This method is recommended over Guice's Modules.override since the later
     * can result in duplicate bindings due to the loss of context for Guice's built in 
     * module de-duping using equals() and hashCode() on a module class.
     * @return
     */
    List<Module> getOverrideModules();
    
    /**
     * Return a list of bootstrap modules that will be loaded prior the main injector
     * being created.  Bootstrap modules should be reserved for things like configuration
     * loading that must be done before any module conditionals may be evaluated. 
     * Applications should normally use getModules()
     */
    List<Module> getBootstrapModules();
    
    /**
     * Return a list of ModuleListProviders through which modules may be auto-loaded.
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
     * Return the main property source to be used during the bootstrap phase 
     * @return
     */
    PropertySource getPropertySources();
    
    /**
     * Determine if a core governator feature has been enabled.  See {@link GovernatorFeatures}
     * for available features.
     */
    boolean isEnabled(GovernatorFeature feature);
}
