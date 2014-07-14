package com.netflix.governator.guice.transformer;

import java.util.Collection;

import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.util.Modules;
import com.netflix.governator.guice.ModuleTransformer;

/**
 * Treat any binding in list order as an override for previous bindings.
 * 
 * @author elandau
 */
public class OverrideAllDuplicateBindings implements ModuleTransformer {
    
    @Override
    public Collection<Module> call(Collection<Module> modules) {
        // Starting point
        Module current = new AbstractModule() {
            @Override
            protected void configure() {
            }
        };

        // Accumulate bindings while allowing for each to override all 
        // previous bindings
        for (Module module : modules) {
            current = Modules.override(current).with(module);
        }
        return ImmutableList.of(current);
    }

}
