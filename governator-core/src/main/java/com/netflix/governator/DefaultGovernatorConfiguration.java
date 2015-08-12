package com.netflix.governator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Module;
import com.google.inject.Stage;
import com.netflix.governator.auto.ModuleListProvider;
import com.netflix.governator.auto.annotations.ConditionalOnProfile;

/**
 * Default implementation of GovernatorConfiguration with mostly empty lists.
 * 
 * @author elandau
 *
 */
public class DefaultGovernatorConfiguration implements GovernatorConfiguration {
    /**
     * Polymorphic builder.
     * 
     * @author elandau
     *
     * @param <T>
     */
    public static abstract class Builder<T extends Builder<T>> {
        protected Stage                       stage = Stage.DEVELOPMENT;
        protected List<Module>                bootstrapModules = new ArrayList<>();
        protected Set<String>                 profiles = new LinkedHashSet<>();
        protected List<ModuleListProvider>    moduleProviders = new ArrayList<>();
        protected Map<GovernatorFeature, Boolean> features = new HashMap<>();
        
        /**
         * Add a module finder such as a ServiceLoaderModuleFinder or ClassPathScannerModuleFinder
         * @param finder
         * @return
         */
        public T addModuleListProvider(ModuleListProvider finder) {
            this.moduleProviders.add(finder);
            return This();
        }
        
        /**
         * Bootstrap overrides for the bootstrap injector used to load and inject into 
         * the conditions.  Bootstrap does not restrict the bindings to allow any type
         * to be externally provided and injected into conditions.  Several simple
         * bindings are provided by default and may be overridden,
         * 1.  Config
         * 2.  Profiles
         * 3.  BoundKeys (TODO)
         * 
         * @param bootstrapModule
         */
        public T addBootstrapModule(Module bootstrapModule) {
            this.bootstrapModules.add(bootstrapModule);
            return This();
        }
        
        public T addBootstrapModules(Module ... bootstrapModule) {
            this.bootstrapModules.addAll(Arrays.asList(bootstrapModule));
            return This();
        }

        public T addBootstrapModules(List<Module> bootstrapModule) {
            this.bootstrapModules.addAll(bootstrapModule);
            return This();
        }

        /**
         * Add a runtime profile.  @see {@link ConditionalOnProfile}
         * 
         * @param profile
         */
        public T addProfile(String profile) {
            this.profiles.add(profile);
            return This();
        }

        /**
         * Add a runtime profiles.  @see {@link ConditionalOnProfile}
         * 
         * @param profile
         */
        public T addProfiles(String... profiles) {
            this.profiles.addAll(Arrays.asList(profiles));
            return This();
        }
        
        /**
         * Add a runtime profiles.  @see {@link ConditionalOnProfile}
         * 
         * @param profile
         */
        public T addProfiles(Collection<String> profiles) {
            this.profiles.addAll(profiles);
            return This();
        }
        
        public T inStage(Stage stage) {
            this.stage = stage;
            return This();
        }
        
        /**
         * Enable the specified feature
         * @param feature
         */
        public T enable(GovernatorFeature feature) {
            this.features.put(feature, true);
            return This();
        }

        /**
         * Disable the specified feature
         * @param feature
         */
        public T disable(GovernatorFeature feature) {
            this.features.put(feature, false);
            return This();
        }
        
        protected abstract T This();
        
        public GovernatorConfiguration build() {
            return new DefaultGovernatorConfiguration(this);
        }
    }
    
    private static class BuilderWrapper extends Builder<BuilderWrapper> {
        @Override
        protected BuilderWrapper This() {
            return this;
        }
    }

    public static Builder<?> builder() {
        return new BuilderWrapper();
    }
    
    private final Stage                       stage;
    private final List<Module>                bootstrapModules;
    private final Set<String>                 profiles;
    private final List<ModuleListProvider>    moduleProviders;
    private final HashMap<GovernatorFeature, Boolean> features;

    public DefaultGovernatorConfiguration() {
        this(builder());
    }
    
    protected DefaultGovernatorConfiguration(Builder<?> builder) {
        this.stage             = builder.stage;
        this.bootstrapModules  = new ArrayList<>(builder.bootstrapModules);
        this.profiles          = new LinkedHashSet<>(builder.profiles);
        this.moduleProviders   = new ArrayList<>(builder.moduleProviders);
        this.features          = new HashMap<>();
        this.features.putAll(builder.features);
    }
    
    @Override
    public List<Module> getBootstrapModules() {
        return bootstrapModules;
    }

    @Override
    public List<ModuleListProvider> getModuleListProviders() {
        return moduleProviders;
    }

    @Override
    public Set<String> getProfiles() {
        return profiles;
    }

    @Override
    public Stage getStage() {
        return stage;
    }

    @Override
    public boolean isEnabled(GovernatorFeature feature) {
        Boolean value = features.get(feature);
        return value == null
                ? feature.isEnabledByDefault()
                : value;
    }
}
