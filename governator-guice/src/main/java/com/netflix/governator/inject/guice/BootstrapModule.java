package com.netflix.governator.inject.guice;

import com.google.inject.Binder;

/**
 * Abstraction for binding during the bootstrap phase
 */
public interface BootstrapModule
{
    /**
     * Called to allow for binding
     *
     * @param binder standard Guice binder
     * @param requiredAssetBinder binder for required assets
     */
    public void configure(Binder binder, RequiredAssetBinder requiredAssetBinder);
}
