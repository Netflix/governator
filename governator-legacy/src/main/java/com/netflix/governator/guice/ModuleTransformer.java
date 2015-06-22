package com.netflix.governator.guice;

import java.util.Collection;

import com.google.inject.Module;

/**
 * Before creating the injector the modules are passed through a collection
 * of filters that can filter out or modify bindings
 * 
 * @author elandau
 *
 */
public interface ModuleTransformer {
    public Collection<Module> call(Collection<Module> modules);
}
