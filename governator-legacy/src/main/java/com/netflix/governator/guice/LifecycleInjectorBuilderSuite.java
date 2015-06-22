package com.netflix.governator.guice;

/**
 * Each concrete LifecycleInjectorSuite represent a specific set operations
 * on a LifecycleInjectorBuilder that should logically be grouped together.
 * Multiple suites can then be applied to the LifecycleInjectorBuilder.
 * 
 * @author elandau
 * 
 * @deprecated This class is deprecated in favor of using {@link BootstrapModule} or just {@link ModuleInfo}.  All the 
 * {@link LifecycleInjectorBuilder} functionality is now available via the {@link BootstrapBinder}
 * passed to {@link BootstrapModule}
 */
@Deprecated
public interface LifecycleInjectorBuilderSuite {
    /**
     * Override this to perform any combination of operations on the
     * LifecycleInjectorBuilder
     * 
     * @param builder
     */
    public void configure(LifecycleInjectorBuilder builder);
}
