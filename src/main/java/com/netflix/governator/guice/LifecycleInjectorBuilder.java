/*
 * Copyright 2013 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.governator.guice;

import java.util.Collection;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.lifecycle.ClasspathScanner;

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
     * Add to any modules already specified via {@link #withModules(Iterable)}
     *
     * @param modules modules
     * @return this
     */
    public LifecycleInjectorBuilder withAdditionalModules(Iterable<? extends Module> modules);

    /**
     * Add to any modules already specified via {@link #withModules(Iterable)}
     *
     * @param modules modules
     * @return this
     */
    public LifecycleInjectorBuilder withAdditionalModules(Module... modules);

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
     * Set the Guice stage - the default is Production
     *
     * @param stage new stage
     * @return this
     */
    public LifecycleInjectorBuilder inStage(Stage stage);

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
     * 
     * @deprecated this API creates the "main" child injector.
     * but it has the side effect of calling build() method 
     * that will create a new LifecycleInjector.
     * Instead, you should just build() LifecycleInjector object.
     * then call LifecycleInjector.createInjector() directly.
     */
    @Deprecated
    public Injector createInjector();
}
