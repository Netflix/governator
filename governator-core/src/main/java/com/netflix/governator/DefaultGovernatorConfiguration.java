package com.netflix.governator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Module;
import com.google.inject.Stage;
import com.netflix.governator.auto.DefaultPropertySource;
import com.netflix.governator.auto.ModuleListProvider;
import com.netflix.governator.auto.PropertySource;
import com.netflix.governator.auto.annotations.ConditionalOnProfile;

/**
 * Default implementation of GovernatorConfiguration with mostly empty lists.
 * 
 * TODO: Should addOverrideModule behavior be that every module can override the previous one?
 * @deprecated Moved to karyon
 */
@Deprecated
public class DefaultGovernatorConfiguration implements GovernatorConfiguration {
    /**
     * Polymorphic builder.
     * 
     * @author elandau
     *
     * @param <T>
     */
    public static abstract class Builder<T extends Builder<T>> {
        protected Stage                       stage             = Stage.DEVELOPMENT;
        protected List<Module>                modules           = new ArrayList<>();
        protected List<Module>                overrideModules   = new ArrayList<>();
        protected PropertySource              propertySource    = new DefaultPropertySource();
        protected Set<String>                 profiles          = new LinkedHashSet<>();
        protected List<ModuleListProvider>    moduleProviders   = new ArrayList<>();
        protected Map<GovernatorFeature, Boolean> features      = new HashMap<>();
        
        /**
         * Add a module finder such as a ServiceLoaderModuleFinder or ClassPathScannerModuleFinder
         * @param finder
         * @return
         */
        public T addModuleListProvider(ModuleListProvider finder) {
            this.moduleProviders.add(finder);
            return This();
        }
        
        public T addModule(Module module) {
            this.modules.add(module);
            return This();
        }
        
        public T addModules(Module ... modules) {
            this.modules.addAll(Arrays.asList(modules));
            return This();
        }
        
        public T addModules(List<Module> modules) {
            this.modules.addAll(modules);
            return This();
        }
        
        public T addOverrideModule(Module module) {
            this.overrideModules.add(module);
            return This();
        }
        
        public T addOverrideModules(Module ... modules) {
            this.overrideModules.addAll(Arrays.asList(modules));
            return This();
        }
        
        public T addOverrideModules(List<Module> modules) {
            this.overrideModules.addAll(modules);
            return This();
        }
        
        /**
         * Add a runtime profile.  @see {@link ConditionalOnProfile}
         * 
         * @param profile
         */
        public T addProfile(String profile) {
            if (profile != null)    
                this.profiles.add(profile);
            return This();
        }

        /**
         * Add a runtime profiles.  @see {@link ConditionalOnProfile}
         * 
         * @param profile
         */
        public T addProfiles(String... profiles) {
            if (profiles != null) {
                this.profiles.addAll(Arrays.asList(profiles));
            }
            return This();
        }
        
        /**
         * Add a runtime profiles.  @see {@link ConditionalOnProfile}
         * 
         * @param profile
         */
        public T addProfiles(Collection<String> profiles) {
            if (profiles != null) {
                this.profiles.addAll(profiles);
            }
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
        
        public T withPropertySource(PropertySource propertySource) {
            this.propertySource = propertySource;
            return This();
        }
        
        protected abstract T This();
        
        protected void initialize() throws Exception {
        }
        
        public GovernatorConfiguration build() throws Exception {
            initialize();
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
    
    public static GovernatorConfiguration createDefault() throws Exception {
        return builder().build();
    }
    
    private final Stage                       stage;
    private final List<Module>                modules;
    private final List<Module>                overrideModules;
    private final Set<String>                 profiles;
    private final List<ModuleListProvider>    moduleProviders;
    private final PropertySource              propertySource;
    private final HashMap<GovernatorFeature, Boolean> features;

    public DefaultGovernatorConfiguration() {
        this(builder());
    }
    
    protected DefaultGovernatorConfiguration(Builder<?> builder) {
        this.stage             = builder.stage;
        this.modules           = new ArrayList<>(builder.modules);
        this.overrideModules   = new ArrayList<>(builder.overrideModules);
        this.profiles          = new LinkedHashSet<>(builder.profiles);
        this.moduleProviders   = new ArrayList<>(builder.moduleProviders);
        this.propertySource    = builder.propertySource;
        this.features          = new HashMap<>();
        this.features.putAll(builder.features);
    }
    
    @Override
    public List<ModuleListProvider> getModuleListProviders() {
        return this.getAutoModuleListProviders();
    }

    @Override
    public List<Module> getModules() {
        return Collections.unmodifiableList(modules);
    }

    @Override
    public List<Module> getOverrideModules() {
        return Collections.unmodifiableList(overrideModules);
    }

    @Override
    public PropertySource getPropertySource() {
        return propertySource;
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
        return isFeatureEnabled(feature);
    }

    @Override
    public List<ModuleListProvider> getAutoModuleListProviders() {
        return Collections.unmodifiableList(moduleProviders);
    }

    @Override
    public boolean isFeatureEnabled(GovernatorFeature feature) {
        Boolean value = features.get(feature);
        return value == null
                ? feature.isEnabledByDefault()
                : value;
    }
}
