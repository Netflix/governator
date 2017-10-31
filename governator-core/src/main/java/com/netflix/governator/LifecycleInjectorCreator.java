package com.netflix.governator;

import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.google.inject.multibindings.Multibinder;
import com.netflix.governator.annotations.SuppressLifecycleUninitialized;
import com.netflix.governator.annotations.binding.Arguments;
import com.netflix.governator.annotations.binding.Profiles;
import com.netflix.governator.internal.DefaultPropertySource;
import com.netflix.governator.internal.GovernatorFeatureSet;
import com.netflix.governator.spi.InjectorCreator;
import com.netflix.governator.spi.LifecycleListener;
import com.netflix.governator.spi.PropertySource;

/**
 * Custom strategy for creating a Guice Injector that enables support for lifecycle annotations such 
 * as {@link @PreDestroy} and {@link @PostConstruct} as well as injector lifecycle hooks via the 
 * {@link LifecycleListener} API. 
 * 
 * The LifecycleInjectorCreator may be overridden to handle pre-create and post-create notification.
 */
public class LifecycleInjectorCreator implements InjectorCreator<LifecycleInjector> {
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleInjectorCreator.class);
    
    private String[] args = new String[]{};
    private LinkedHashSet<String> profiles = new LinkedHashSet<>();
    private IdentityHashMap<GovernatorFeature<?>, Object> features = new IdentityHashMap<>();
    
    public LifecycleInjectorCreator withArguments(String[] args) {
        Preconditions.checkArgument(args != null, "Arg may not be null");
        this.args = args;
        return this;
    }
    
    public LifecycleInjectorCreator withProfiles(String... profiles) {
        Preconditions.checkArgument(profiles != null, "Arg may not be null");
        this.profiles = new LinkedHashSet<>(Arrays.asList(profiles));
        return this;
    }
    
    public LifecycleInjectorCreator withProfiles(Set<String> profiles) {
        Preconditions.checkArgument(profiles != null, "profiles may not be null");
        this.profiles = new LinkedHashSet<>(profiles);
        return this;
    }
    
    public LifecycleInjectorCreator withFeatures(IdentityHashMap<GovernatorFeature<?>, Object> features) {
        Preconditions.checkArgument(features != null, "features may not be null");
        this.features = features;
        return this;
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

    @Override
    public LifecycleInjector createInjector(Stage stage, Module module) {
        final GovernatorFeatureSetImpl featureSet = new GovernatorFeatureSetImpl(features);
        
        final LifecycleManager manager = new LifecycleManager();
        
        // Construct the injector using our override structure
        try {
            onBeforeInjectorCreate();
            Injector injector = Guice.createInjector(
                stage, 
                // This has to be first to make sure @PostConstruct support is added as early
                // as possible
                new ProvisionMetricsModule(), 
                new LifecycleModule(),
                new LifecycleListenerModule(),
                new LegacyScopesModule(),
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(GovernatorFeatureSet.class).toInstance(featureSet);
                        bind(LifecycleManager.class).toInstance(manager);
                        Multibinder<String> profilesBinder = Multibinder.newSetBinder(binder(), Key.get(String.class, Profiles.class)).permitDuplicates();
                        profiles.forEach(profile -> profilesBinder.addBinding().toInstance(profile));
                        bind(String[].class).annotatedWith(Arguments.class).toInstance(args);
                        requestInjection(LifecycleInjectorCreator.this);
                    }
                },
                module
                );
            manager.notifyStarted();
            LifecycleInjector lifecycleInjector = LifecycleInjector.wrapInjector(injector, manager);
            onSuccessfulInjectorCreate();
            LOG.info("Injector created successfully ");
            return lifecycleInjector;
        }
        catch (Exception e) {
            LOG.error("Failed to create injector - {}@{}", e.getClass().getSimpleName(), System.identityHashCode(e), e);
            onFailedInjectorCreate(e);
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
        finally {
            onCompletedInjectorCreate();
        }
    }

    /**
     * Template method invoked immediately before the injector is created
     */
    protected void onBeforeInjectorCreate() {
    }

    /**
     * Template method invoked immediately after the injector is created
     */
    protected void onSuccessfulInjectorCreate() {
    }
    
    /**
     * Template method invoked immediately after any failure to create the injector
     * @param error Cause of the failure
     */
    protected void onFailedInjectorCreate(Throwable error) {
    }

    /**
     * Template method invoked at the end of createInjector() regardless of whether
     * the injector was created successful or not.
     */
    protected void onCompletedInjectorCreate() {
        
    }
    
    @Override
    public String toString() {
        return "LifecycleInjectorCreator[]";
    }
}
