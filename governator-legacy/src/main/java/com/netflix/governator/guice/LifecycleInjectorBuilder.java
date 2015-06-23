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

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.lifecycle.ClasspathScanner;
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
     * Specify additional bootstrap modules to use
     *
     * @param modules modules
     * @return this
     */
    public LifecycleInjectorBuilder withAdditionalBootstrapModules(BootstrapModule... modules);

    /**
     * Specify additional bootstrap modules to use
     *
     * @param modules modules
     * @return this
     */
    public LifecycleInjectorBuilder withAdditionalBootstrapModules(Iterable<? extends BootstrapModule> modules);

    /**
     * Specify standard Guice modules for the main binding phase.  Note that any
     * modules provided in a previous call to withModules will be discarded.
     * To add to the list of modules call {@link #withAdditionalModules}
     *
     * @param modules modules
     * @return this
     */
    public LifecycleInjectorBuilder withModules(Module... modules);

    /**
     * Specify standard Guice modules for the main binding phase. Note that any
     * modules provided in a previous call to withModules will be discarded.
     * To add to the list of modules call {@link #withAdditionalModules}
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
     * Specify a root application module class from which a set of additional modules
     * may be derived using module dependencies. Module dependencies are specified
     * using @Inject on the module constructor and indicating the dependent modules
     * as constructor arguments.
     *
     * @param mainModule root application module
     * @return this
     */
    @Deprecated
    public LifecycleInjectorBuilder withRootModule(Class<?> mainModule);

    /**
     * Specify a module class from which a set of additional modules may be derived
     * using module dependencies. Module dependencies are specified
     * using @Inject on the module constructor and indicating the dependent modules
     * as constructor arguments.
     *
     * @param module root application module
     * @return this
     */
    public LifecycleInjectorBuilder withModuleClass(Class<? extends Module> module);

    /**
     * Specify a set of module classes from which a set of additional modules may be derived
     * using module dependencies. Module dependencies are specified
     * using @Inject on the module constructor and indicating the dependent modules
     * as constructor arguments.
     *
     * Note that any existing modules that were added will be removed by this call
     *
     * @param modules root application modules
     * @return this
     */
    public LifecycleInjectorBuilder withModuleClasses(Iterable<Class<? extends Module>> modules);

    /**
     * Specify a set of module classes from which a set of additional modules may be derived
     * using module dependencies. Module dependencies are specified
     * using @Inject on the module constructor and indicating the dependent modules
     * as constructor arguments.
     *
     * Note that any existing modules that were added will be removed by this call
     *
     * @param modules root application modules
     * @return this
     */
    public LifecycleInjectorBuilder withModuleClasses(Class<?> ... modules);

    /**
     * Specify a set of module classes from which a set of additional modules may be derived
     * using module dependencies. Module dependencies are specified
     * using @Inject on the module constructor and indicating the dependent modules
     * as constructor arguments.
     *
     * Note that any existing modules that were added will be removed by this call
     * @param modules root application modules
     * @return this
     */
    public LifecycleInjectorBuilder withAdditionalModuleClasses(Iterable<Class<? extends Module>> modules);

    /**
     * Specify a set of module classes from which a set of additional modules may be derived
     * using module dependencies. Module dependencies are specified
     * using @Inject on the module constructor and indicating the dependent modules
     * as constructor arguments.
     *
     * @param modules root application modules
     * @return this
     */
    public LifecycleInjectorBuilder withAdditionalModuleClasses(Class<?> ... modules);
    
    /**
     * When using module dependencies ignore the specified classes
     * 
     * @param modules to exclude
     * @return this
     */
    public LifecycleInjectorBuilder withoutModuleClasses(Iterable<Class<? extends Module>> modules);
    
    /**
     * When using module dependencies ignore the specified classes
     * @param modules to exclude
     * @return this
     */
    public LifecycleInjectorBuilder withoutModuleClasses(Class<? extends Module> ... modules);
    
    /**
     * When using module dependencies ignore the specified class
     * @param module to exclude
     * @return this
     */
    public LifecycleInjectorBuilder withoutModuleClass(Class<? extends Module> module);
    
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
     * Set the lifecycle injector mode - default is {@link LifecycleInjectorMode#REAL_CHILD_INJECTORS}
     *
     * @param mode new mode
     * @return this
     */
    public LifecycleInjectorBuilder withMode(LifecycleInjectorMode mode);

    /**
     * Just before creating the injector all the modules will run through the transformer.
     * Transformers will be executed in the order in which withModuleTransformer
     * is called.  Note that once the first filter is called subsequent calls will only be
     * given the previous set of filtered modules.
     *
     * @param transformer
     * @return this
     */
    public LifecycleInjectorBuilder withModuleTransformer(ModuleTransformer transformer);

    /**
     * Just before creating the injector all the modules will run through the filter.
     * Transformers will be executed in the order in which withModuleTransformer
     * is called.  Note that once the first filter is called subsequent calls will only be
     * given the previous set of filtered modules.
     *
     * @param transformer
     * @return this
     */
    public LifecycleInjectorBuilder withModuleTransformer(Collection<? extends ModuleTransformer> transformer);

    /**
     * Just before creating the injector all the modules will run through the filter.
     * Transformers will be executed in the order in which withModuleTransformer
     * is called.  Note that once the first filter is called subsequent calls will only be
     * given the previous set of filtered modules.
     *
     * @param transformer
     * @return this
     */
    public LifecycleInjectorBuilder withModuleTransformer(ModuleTransformer... transformer);

    /**
     * Action to perform after the injector is created.  Note that post injection actions
     * are performed in the same order as calls to withPostInjectorAction
     * @param action
     * @return
     */
    public LifecycleInjectorBuilder withPostInjectorAction(PostInjectorAction action);

    /**
     * Actions to perform after the injector is created.  Note that post injection actions
     * are performed in the same order as calls to withPostInjectorAction
     * @param action
     * @return
     */
    public LifecycleInjectorBuilder withPostInjectorActions(Collection<? extends PostInjectorAction> action);

    /**
     * Actions to perform after the injector is created.  Note that post injection actions
     * are performed in the same order as calls to withPostInjectorAction
     * @param action
     * @return
     */
    public LifecycleInjectorBuilder withPostInjectorActions(PostInjectorAction... actions);

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
