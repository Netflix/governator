package com.netflix.governator;

import java.util.Arrays;
import java.util.List;

import com.google.inject.Module;
import com.netflix.governator.auto.ClassPathConditionalModuleListProvider;

/**
 * Utility class with convenience methods for creating various standard
 * {@link ModuleListProvider} implementations.
 * 
 * @author elandau
 *
 */
public class ModuleListProviders {
    /**
     * Provider for a fixed pre-defined list of modules
     * @param modules
     * @return
     */
    public static ModuleListProvider forModules(final Module... modules) {
        return forModules(Arrays.asList(modules));
    }
    
    /**
     * Provider for a fixed pre-defined list of modules
     * @param modules
     * @return
     */
    public static ModuleListProvider forModules(final List<Module> modules) {
        return new ModuleListProvider() {
            @Override
            public List<Module> get() {
                return modules;
            }
        };
    }
    
    /**
     * Provider that will use Guava's ClassPath scanner to scan the provided 
     * packages.
     * 
     * @param modules
     * @return
     */
    public static ModuleListProvider forPackages(final String... packages) {
        return new ClassPathModuleListProvider(packages);
    }
    
    public static ModuleListProvider forPackagesConditional(final String... packages) {
        return new ClassPathConditionalModuleListProvider(packages);
    }
        
    /**
     * Provider that will use Guava's ClassPath scanner to scan the provided 
     * packages.
     * 
     * @param modules
     * @return
     */
    public static ModuleListProvider forPackages(final List<String> packages) {
        return new ClassPathModuleListProvider(packages);
    }
    
    public static ModuleListProvider forPackagesConditional(List<String> packages) {
        return new ClassPathConditionalModuleListProvider(packages);
    }
        
    /**
     * Provider using the ServiceLoader for class Module
     * 
     * @param modules
     * @return
     */
    public static ModuleListProvider forServiceLoader() {
        return new ServiceLoaderModuleListProvider();
    }
    
    /**
     * Provider using the ServiceLoader for class Module
     * 
     * @param modules
     * @return
     */
    public static ModuleListProvider forServiceLoader(Class<? extends Module> type) {
        return new ServiceLoaderModuleListProvider(type);
    }
}
