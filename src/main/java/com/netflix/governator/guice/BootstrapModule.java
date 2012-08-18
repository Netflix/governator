package com.netflix.governator.guice;

/**
 * Abstraction for binding during the bootstrap phase
 */
public interface BootstrapModule
{
    /**
     * Called to allow for binding
     *
     * @param binder the bootstrap binder
     */
    public void configure(BootstrapBinder binder);
}
