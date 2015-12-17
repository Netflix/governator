package com.netflix.governator.spi;

import com.google.inject.Module;

/**
 * Mapping function from one module to another.  A transformer could perform operations
 * such as logging, removing dependencies or auto-generating bindings.
 */
public interface ModuleTransformer {
    Module transform(Module module);
}
