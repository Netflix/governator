package com.netflix.governator.guice;

import java.lang.reflect.Constructor;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.InjectionPoint;
import com.netflix.governator.annotations.Modules;

/**
 * The {@link ModuleListBuilder} keeps track of modules and their transitive dependencies
 * and provides a mechanism to replace or exclude modules. 
 * 
 * When {@link build()} is called a list of modules is created and modules will be ordered
 * in the order in which they were added while allowing for dependent modules to be listed
 * first.
 * 
 * TODO: Provide exclude source
 * TODO: Provide include source
 * TODO: Force include
 * TODO: Guard against circular dependencies
 * 
 * @author elandau
 */
public class ModuleListBuilder {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleListBuilder.class);
    
    /**
     * Internal class to track either a module class or instance
     * @author elandau
     */
    public class ModuleProvider {
        Class<? extends Module> type;
        Module instance;
        boolean isExcluded = false;
        
        public ModuleProvider(Class<? extends Module> type) {
            this.type = type;
            instance = null;
        }
        
        public ModuleProvider(Module instance) {
            this.instance = instance;
            if (instance instanceof AbstractModule) {
                type = null;
            }
        }
        
        public void setInstance(Module module) {
            Preconditions.checkState(instance == null, "Instance already exists");
            this.instance = module;
        }
        
        public Module getInstance(Injector injector) throws Exception {
            try {
                // Already created
                if (instance != null) {
                    return instance;
                }
                
                LOG.info("Getting instance of : " + type.getName());
                if (excludes.contains(type)) {
                    LOG.info("Module '" + type.getName() + "' is excluded");
                    return null;
                }
                
                // Create all of this modules dependencies.  This includes both @Modules and injected
                // dependencies
                for (Class<? extends Module> dep : getIncludeList()) {
                    ModuleProvider provider = includes.get(dep);
                    provider.getInstance(injector);
                }
                
                // If @Inject is present then instantiate using that constructor and manually inject
                // the dependencies.  Note that a null will be injected for excluded modules
                try {
                    InjectionPoint ip = InjectionPoint.forConstructorOf(type);
                    if (ip != null) {
                        Constructor<?> c = (Constructor<?>) ip.getMember();
                        c.setAccessible(true);
                        List<Dependency<?>> deps = ip.getDependencies();
                        if (!deps.isEmpty()) {
                            Object[] args = new Object[deps.size()];
                            for (Dependency<?> dep : deps) {
                                Class<?> type = dep.getKey().getTypeLiteral().getRawType();
                                if (Module.class.isAssignableFrom(type)) {
                                    args[dep.getParameterIndex()] = includes.get(dep.getKey().getTypeLiteral().getRawType()).getInstance(injector);
                                }
                                else {
                                    args[dep.getParameterIndex()] = injector.getInstance(dep.getKey());
                                }
                            }
                            c.setAccessible(true);
                            
                            instance = (Module) c.newInstance(args);
                        }
                        else {
                        	instance = (Module) c.newInstance();
                        }
                    }
                    else {
	                    // Empty constructor
	                    Constructor<?> c = type.getConstructor();
	                    if (c != null) {
		                    c.setAccessible(true);
		                    instance = (Module) c.newInstance();
	                    }
	                    // Default constructor
	                    else {
	                    	instance = type.newInstance();
	                    }
                    }
                    injector.injectMembers(instance);
                    return instance;
                }
                catch (Exception e) {
                    throw new ProvisionException("Failed to create module '" + type.getName() + "'", e);
                }
            }
            finally {
                if (instance != null) {
                    registerModule(instance);
                }
            }
        }
        
        private List<Class<? extends Module>> getIncludeList() {
            // Look for @Modules(includes={..})
            Builder<Class<? extends Module>> builder = ImmutableList.<Class<? extends Module>>builder();
            if (type != null) {
                Modules annot = type.getAnnotation(Modules.class);
                if (annot != null && annot.include() != null) {
                    builder.add(annot.include());
                }
                
                // Look for injected modules
                for (Dependency<?> dep : InjectionPoint.forConstructorOf(type).getDependencies()) {
                    Class<?> depType = dep.getKey().getTypeLiteral().getRawType();
                    if (Module.class.isAssignableFrom(depType)) {
                        builder.add((Class<? extends Module>) depType);
                    }
                }
            }
            
            return builder.build();
        }
        
        private List<Class<? extends Module>> getExcludeList() {
            Builder<Class<? extends Module>> builder = ImmutableList.<Class<? extends Module>>builder();
            if (type != null) {
                Modules annot = type.getAnnotation(Modules.class);
                if (annot != null && annot.exclude() != null) {
                    builder.add(annot.exclude());
                }
            }
            return builder.build();
        }

    }
    
    // List of all identified Modules in the order in which they were added and identified
    private List<ModuleProvider> providers = Lists.newArrayList();
    
    // Map of seen class to the provider.  Note that this map will not include any module
    // that is a simple 
    private Map<Class<? extends Module>, ModuleProvider> includes = Maps.newIdentityHashMap();
    
    // Set of module classes to exclude
    private Set<Class<? extends Module>> excludes = Sets.newIdentityHashSet();
    
    // Final list of modules to install
    private List<Module> resolvedModules = Lists.newArrayList();
    
    // Lookup of resolved modules for duplicate check
    private Set<Class<? extends Module>> resolvedModuleLookup = Sets.newIdentityHashSet();
    
    public ModuleListBuilder includeModules(Iterable<? extends Module> modules) {
        for (Module module : modules) {
            include (module);
        }
        return this;
    }

    public ModuleListBuilder include(Iterable<Class<? extends Module>> modules) {
        for (Class<? extends Module> module : modules) {
            include (module);
        }
        return this;
    }

    public ModuleListBuilder include(final Module m) {
        ModuleProvider provider = new ModuleProvider(m);
        
        if (!m.getClass().isAnonymousClass()) {
            // Do nothing if already exists
            if (includes.containsKey(m.getClass())) {
                return this;
            }
            
            includes.put(m.getClass(),  provider);
            
            // Get all @Modules and injected dependencies of this module
            for (Class<? extends Module> dep : provider.getIncludeList()) {
                // Circular dependencies will be caught by includes.containsKey() above
                include(dep, false);
            }
            
            for (Class<? extends Module> dep : provider.getExcludeList()) {
                exclude(dep);
            }
        }

        // Add to list of known modules.  We do this after all dependencies
        // have been added
        providers.add(provider);

        return this;
    }
    
    public ModuleListBuilder include(final Class<? extends Module> m) {
        include(m, true);
        return this;
    }
    
    private void include(final Class<? extends Module> m, boolean addToProviders) {
        ModuleProvider provider = new ModuleProvider(m);
        
        if (!m.getClass().isAnonymousClass()) {
            // Do nothing if already exists
            if (includes.containsKey(m.getClass())) {
                return;
            }
            
            includes.put(m,  provider);
            
            // Get all @Modules and injected dependencies of this module
            for (Class<? extends Module> dep : provider.getIncludeList()) {
                // Circular dependencies will be caught by includes.containsKey() above
                include(dep, false);
            }
            
            for (Class<? extends Module> dep : provider.getExcludeList()) {
                exclude(dep);
            }
        }

        // Add to list of known modules.  We do this after all dependencies
        // have been added
        if (addToProviders) {
            providers.add(provider);
        }
    }
    
    public ModuleListBuilder exclude(Class<? extends Module> m) {
        excludes.add(m);
        return this;
    }
    
    public ModuleListBuilder exclude(Iterable<Class<? extends Module>> modules) {
        for (Class<? extends Module> module : modules) {
            excludes.add(module);
        }
        return this;
    }

    private void registerModule(Module m) {
        if (m.getClass().isAnonymousClass()) {
            resolvedModules.add(m);
        }
        else {
            if (!resolvedModuleLookup.contains(m.getClass())) {
                LOG.info("Adding module '" + m.getClass().getName());
                resolvedModules.add(m);
                resolvedModuleLookup.add(m.getClass());
            }
        }
    }
    
    List<Module> build(Injector injector) throws Exception {
        for (ModuleProvider provider : providers) {
            provider.getInstance(injector);
        }
        return resolvedModules;
    }
}
