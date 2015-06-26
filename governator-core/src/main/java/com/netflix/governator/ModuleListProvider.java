package com.netflix.governator;

import java.util.List;

import com.google.inject.Module;
import com.netflix.governator.auto.AutoModuleBuilder;

/**
 * Plugin interface for a module provider, such as a ClassPathScannerModuleProvider or 
 * a ServiceLoaderModuleProvider.
 * 
 * @see AutoModuleBuilder
 * @author elandau
 */
public interface ModuleListProvider {
    /**
     * 
     * @return
     */
    List<Module> get();
}
