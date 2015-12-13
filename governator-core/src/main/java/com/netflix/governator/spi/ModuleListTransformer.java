package com.netflix.governator.spi;

import java.util.List;

import com.google.inject.Module;

/**
 * Module transformers are used to modify the final list of modules and return a 
 * modified or augmented list of modules which may included additional or removed
 * bindings.
 * 
 * ModuleTransfomers can be used to do the following types of functionality,
 * 1.  Remove unwanted bindings
 * 2.  Auto add non-existent bindings 
 * 3.  Warn on dangerous bindings like toInstance() and static injection.
 */
public interface ModuleListTransformer {
    /**
     * Using the provided list of modules (and bindings) return a new augments list
     * which may included additional bindings and modules.  
     * 
     * @param modules
     * @return New list of modules.  Can be the old list.
     */
    List<Module> transform(List<Module> modules);
}
