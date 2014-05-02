package com.netflix.governator.guice;

/**
 * Each concrete LifecycleInjectorSuite represent a specific set operations
 * on a LifecycleInjectorBuilder that should logically be grouped together.
 * Multiple suites can then be applied to the LifecycleInjectorBuilder.
 * 
 * @author elandau
 *
 */
public interface LifecycleInjectorBuilderSuite {
    /**
     * Override this to perform any combination of operations on the
     * LifecycleInjectorBuilder
     * 
     * @param builder
     */
    public void configure(LifecycleInjectorBuilder builder);
}
