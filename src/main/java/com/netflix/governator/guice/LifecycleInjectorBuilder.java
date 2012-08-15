package com.netflix.governator.guice;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleListener;
import java.util.Collection;

/**
 * Builder for a {@link LifecycleInjector}
 */
public interface LifecycleInjectorBuilder
{
    /**
     * Specify a bootstrap module
     *
     * @param module the module
     * @return this
     */
    public LifecycleInjectorBuilder withBootstrapModule(BootstrapModule module);

    /**
     * Specify standard Guice modules for the main binding phase
     *
     * @param modules modules
     * @return this
     */
    public LifecycleInjectorBuilder withModules(Module... modules);

    /**
     * Specify standard Guice modules for the main binding phase
     *
     * @param modules modules
     * @return this
     */
    public LifecycleInjectorBuilder withModules(Iterable<? extends Module> modules);

    /**
     * Specify specific {@link AutoBindSingleton} classes that should NOT be bound in the main
     * binding phase
     *
     * @param ignoreClasses classes to not bind
     * @return this
     */
    public LifecycleInjectorBuilder ignoringAutoBindClasses(Collection<Class<?>> ignoreClasses);

    /**
     * Do not bind ANY {@link AutoBindSingleton} classes
     *
     * @return this
     */
    public LifecycleInjectorBuilder ignoringAllAutoBindClasses();

    /**
     * Specify the base packages for CLASSPATH scanning. Packages are recursively scanned
     *
     * @param basePackages packages
     * @return this
     */
    public LifecycleInjectorBuilder usingBasePackages(String... basePackages);

    /**
     * Specify the base packages for CLASSPATH scanning. Packages are recursively scanned
     *
     * @param basePackages packages
     * @return this
     */
    public LifecycleInjectorBuilder usingBasePackages(Collection<String> basePackages);

    /**
     * Normally, the classpath scanner is allocated internally. This method allows for a custom
     * scanner to be used. NOTE: Any packages specifies via {@link #usingBasePackages(String...)} will
     * be ignored if this method is called.
     *
     * @param scanner the scanner to use
     * @return this
     */
    public LifecycleInjectorBuilder usingClasspathScanner(ClasspathScanner scanner);

    /**
     * Specify a listener
     *
     * @param lifecycleListener the listener
     * @return this
     */
    public LifecycleInjectorBuilder withLifecycleListener(LifecycleListener lifecycleListener);

    /**
     * Build and return the injector
     *
     * @return LifecycleInjector
     */
    public LifecycleInjector build();

    /**
     * Internally build the LifecycleInjector and then return the result of calling
     * {@link LifecycleInjector#createInjector()}
     *
     * @return Guice injector
     */
    public Injector createInjector();
}
