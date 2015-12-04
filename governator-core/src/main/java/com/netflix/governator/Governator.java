package com.netflix.governator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.ConfigurationException;
import com.google.inject.CreationException;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Modules;
import com.netflix.governator.annotations.SuppressLifecycleUninitialized;
import com.netflix.governator.annotations.binding.Arguments;
import com.netflix.governator.annotations.binding.Profiles;
import com.netflix.governator.internal.DefaultPropertySource;
import com.netflix.governator.internal.GovernatorFeatureSet;
import com.netflix.governator.internal.ModulesEx;
import com.netflix.governator.spi.ModuleListTransformer;
import com.netflix.governator.spi.PropertySource;

/**
 * Main entry point for creating a LifecycleInjector with guice extensions such as 
 * conditional bindings.  
 * 
 * This injector is constructed in two phases.  The first bootstrap phase determines which core
 * Guice modules should be installed based on processing of conditional annotations.  The final
 * list of auto discovered modules is appended to the main list of modules and installed on the
 * main injector.  
 * 
 * <code>
     Governator.newBuilder()
        .addModules(
            new ArchaiusGovernatorModule(),
            new JettyModule(),
            new JerseyServletModule() {
                {@literal @}@Override
                protected void configureServlets() {
                    serve("/*").with(GuiceContainer.class);
                    bind(GuiceContainer.class).asEagerSingleton();
                    
                    bind(HelloWorldApp.class).asEagerSingleton();
                }  
            }
        )
        .start()
        .awaitTermination();
 * </code>
 */
public class Governator {
    private static final Stage LAZY_SINGLETONS_STAGE = Stage.DEVELOPMENT;
    
    protected Set<String>                 profiles          = new LinkedHashSet<>();
    protected List<Module>                modules           = new ArrayList<>();
    protected List<ModuleListTransformer> transformers      = new ArrayList<>();
    protected List<Module>                overrideModules   = new ArrayList<>();
    protected IdentityHashMap<GovernatorFeature<?>, Object> featureOverrides  = new IdentityHashMap<>();
    
    // This is a hack to make sure that if archaius is used at some point we make use
    // of the bridge so any access to the legacy Archaius1 API is actually backed by 
    // the Archaius2 implementation
    static {
        System.setProperty("archaius.default.configuration.class",      "com.netflix.archaius.bridge.StaticAbstractConfiguration");
        System.setProperty("archaius.default.deploymentContext.class",  "com.netflix.archaius.bridge.StaticDeploymentContext");
    }
    
    @Singleton
    @SuppressLifecycleUninitialized
    class GovernatorFeatureSetImpl implements GovernatorFeatureSet {
        private final IdentityHashMap<GovernatorFeature<?>, Object> featureOverrides;
        
        @Inject
        private PropertySource properties = new DefaultPropertySource();
        
        @Inject
        public GovernatorFeatureSetImpl(IdentityHashMap<GovernatorFeature<?>, Object> featureOverrides) {
            this.featureOverrides = featureOverrides;
        }
        
        @SuppressWarnings("unchecked")
        @Override
        public <T> T get(GovernatorFeature<T> feature) {
            return featureOverrides.containsKey(feature)
                ? (T) featureOverrides.get(feature)
                : (T) properties.get(feature.getKey(), feature.getType(), feature.getDefaultValue());
        }
    }
    
    /**
     * Add Guice modules to karyon.  
     * 
     * @param modules Guice modules to add.  
     * @return this
     */
    public Governator addModules(Module ... modules) {
        if (modules != null) {
            this.modules.addAll(Arrays.asList(modules));
        }
        return this;
    }
    
    /**
     * Add Guice modules to karyon.  
     * 
     * @param modules Guice modules to add.  
     * @return this
     */
    public Governator addModules(List<Module> modules) {
        if (modules != null) {
            this.modules.addAll(modules);
        }
        return this;
    }
    
    /**
     * Add a runtime profile.  Profiles are processed by the conditional binding {@literal @}ConditionalOnProfile and
     * are injectable as {@literal @}Profiles Set{@literal <}String{@literal >}.
     * 
     * @param profile A profile
     * @return this
     */
    public Governator addProfile(String profile) {
        if (profile != null) {
            this.profiles.add(profile);
        }
        return this;
    }

    /**
     * Add a runtime profiles.  Profiles are processed by the conditional binding {@literal @}ConditionalOnProfile and
     * are injectable as {@literal @}Profiles Set{@literal <}String{@literal >}.
     * 
     * @param profiles Set of profiles
     * @return this
     */
    public Governator addProfiles(String... profiles) {
        if (profiles != null) {
            this.profiles.addAll(Arrays.asList(profiles));
        }
        return this;
    }
    
    /**
     * Add a runtime profiles.  Profiles are processed by the conditional binding {@literal @}ConditionalOnProfile and
     * are injectable as {@literal @}Profiles Set{@literal <}String{@literal >}.
     * 
     * @param profiles Set of profiles
     * @return this
     */
    public Governator addProfiles(Collection<String> profiles) {
        if (profiles != null) {
            this.profiles.addAll(profiles);
        }
        return this;
    }
    
    /**
     * Enable the specified feature
     * @param feature Boolean feature to enable
     * @return this
     */
    public Governator enableFeature(GovernatorFeature<Boolean> feature) {
        return setFeature(feature, true);
    }
    
    /**
     * Enable or disable the specified feature
     * @param feature Boolean feature to disable
     * @return this
     */
    public Governator enableFeature(GovernatorFeature<Boolean> feature, boolean enabled) {
        return setFeature(feature, enabled);
    }

    /**
     * Disable the specified feature
     * @param feature Boolean feature to enable/disable
     * @return this
     */
    public Governator disableFeature(GovernatorFeature<Boolean> feature) {
        return setFeature(feature, false);
    }
    
    /**
     * Set a feature
     * @param feature Feature to set
     * @return this
     */
    public <T> Governator setFeature(GovernatorFeature<T> feature, T value) {
        this.featureOverrides.put(feature, value);
        return this;
    }
    
    /**
     * Add a ModuleListTransformer that will be invoked on the final list of modules
     * prior to creating the injectors.  Multiple transformers may be added with each
     * transforming the result of the previous one.
     * 
     * @param transformer A transformer
     * @return this
     */
    public Governator addModuleListTransformer(ModuleListTransformer transformer) {
        if (transformer != null) {
            this.transformers.add(transformer);
        }
        return this;
    }
    
    /**
     * Add override modules for any modules add via addModules or that are 
     * conditionally loaded.  This is useful for testing or when an application
     * absolutely needs to override a binding to fix a binding problem in the
     * code modules
     * @param modules Modules that will be applied as overrides to modules add
     *  or installed via {@link Karyon#addModules(Module...)}
     * @return this
     */
    public Governator addOverrideModules(Module ... modules) {
        if (modules != null) {
            this.overrideModules.addAll(Arrays.asList(modules));
        }
        return this;
    }
    
    /**
     * Add override modules for any modules add via addModules or that are 
     * conditionally loaded.  This is useful for testing or when an application
     * absolutely needs to override a binding to fix a binding problem in the
     * code modules
     * @param modules Modules that will be applied as overrides to modules add
     *  or installed via {@link Karyon#addModules(Module...)}
     * @return this
     */
    public Governator addOverrideModules(List<Module> modules) {
        if (modules != null) {
            this.overrideModules.addAll(modules);
        }
        return this;
    }

    /**
     * Create the injector and call any LifecycleListeners
     * @return the LifecycleInjector for this run
     */
    public LifecycleInjector run() {
        return run(ModulesEx.emptyModule(), new String[]{});
    }
    
    public LifecycleInjector run(final String[] args) {
        return run(ModulesEx.emptyModule(), args);
    }
    
    public LifecycleInjector run(final Class<? extends LifecycleListener> mainClass) {
        return run(mainClass, new String[]{});
    }
    
    public LifecycleInjector run(LifecycleListener mainClass) {
        return run(ModulesEx.fromInstance(mainClass), new String[]{});
    }
    
    public LifecycleInjector run(LifecycleListener mainClass, final String[] args) {
        return run(ModulesEx.fromInstance(mainClass), args);
    }
    
    public LifecycleInjector run(final Class<? extends LifecycleListener> mainClass, final String[] args) {
        return run(ModulesEx.fromEagerSingleton(mainClass), args);
    }
    
    /**
     * Create the injector and call any LifecycleListeners
     * @param args - Runtime parameter (from main) injectable as {@literal @}Arguments String[]
     * @return the LifecycleInjector for this run
     */
    private LifecycleInjector run(Module externalModule, final String[] args) {
        final Logger LOG = LoggerFactory.getLogger(Governator.class);
        
        final GovernatorFeatureSetImpl featureSet = new GovernatorFeatureSetImpl(new IdentityHashMap<>(featureOverrides));
        
        final LifecycleManager manager = new LifecycleManager();
        
        // Construct the injector using our override structure
        try {
            List<Module> coreModules = new ArrayList<>();
            coreModules.addAll(modules);
            coreModules.add(externalModule);
            coreModules.add(new LifecycleModule());
            coreModules.add(new LegacyScopesModule());
            coreModules.add(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(GovernatorFeatureSet.class).toInstance(featureSet);
                    bind(LifecycleManager.class).toInstance(manager);
                    bind(new TypeLiteral<Set<String>>() {}).annotatedWith(Profiles.class).toInstance(profiles);
                    bind(String[].class).annotatedWith(Arguments.class).toInstance(args);
                }
            });
            
            List<ModuleListTransformer> localTransformers = new ArrayList<ModuleListTransformer>(transformers);
            localTransformers.add(new BindingLoggingModuleTransformer());
            
            for (ModuleListTransformer transformer : localTransformers) {
                coreModules = transformer.transform(Collections.unmodifiableList(coreModules));
            }

            Injector injector = Guice.createInjector(
                LAZY_SINGLETONS_STAGE,
                Modules.override(coreModules).with(overrideModules)
            );
            manager.notifyStarted();
            return LifecycleInjector.wrapInjector(injector, manager);
        }
        catch (ProvisionException|CreationException|ConfigurationException e) {
            LOG.error("Failed to create injector", e);
            try {
                manager.notifyStartFailed(e);
            }
            catch (Exception e2) {
                LOG.error("Failed to notify injector creation failure", e2 );
            }
            if (!featureSet.get(GovernatorFeatures.SHUTDOWN_ON_ERROR)) {
                return LifecycleInjector.createFailedInjector(manager);
            }
            else {
                throw e;
            }
        }
    }
}
