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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleManager;
import javax.annotation.Resource;
import javax.annotation.Resources;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/**
 * <p>
 *     When using Governator, do NOT create a Guice injector manually. Instead, use a LifecycleInjector to create a Guice injector.
 * </p>
 *
 * <p>
 *     Governator uses a two pass binding. The bootstrap binding injects:
 *     <li>{@link LifecycleManager}</li>
 *     <li>Any application defined bootstrap instances</li>
 *     <br/>
 *     The main binding injects everything else.
 * </p>
 *
 * <p>
 *     The bootstrap binding occurs when the LifecycleInjector is created. The main binding
 *     occurs when {@link #createInjector()} is called.
 * </p>
 */
public class LifecycleInjector
{
    private final ClasspathScanner scanner;
    private final List<Module> modules;
    private final Collection<Class<?>> ignoreClasses;
    private final boolean ignoreAllClasses;
    private final LifecycleManager lifecycleManager;
    private final Injector injector;
    private final List<Module> discoveredModules = Lists.newArrayList();

    /**
     * Create a new LifecycleInjector builder
     *
     * @return builder
     */
    public static LifecycleInjectorBuilder builder()
    {
        return new LifecycleInjectorBuilderImpl();
    }

    /**
     * If you need early access to the CLASSPATH scanner. For performance reasons, you should
     * pass the scanner to the builder via {@link LifecycleInjectorBuilder#usingClasspathScanner(ClasspathScanner)}.
     *
     * @param basePackages packages to recursively scan
     * @return scanner
     */
    public static ClasspathScanner createStandardClasspathScanner(Collection<String> basePackages)
    {
        return createStandardClasspathScanner(basePackages, null);
    }

    /**
     * If you need early access to the CLASSPATH scanner. For performance reasons, you should
     * pass the scanner to the builder via {@link LifecycleInjectorBuilder#usingClasspathScanner(ClasspathScanner)}.
     *
     * @param basePackages packages to recursively scan
     * @param additionalAnnotations any additional annotations to scan for
     * @return scanner
     */
    public static ClasspathScanner createStandardClasspathScanner(Collection<String> basePackages, List<Class<? extends Annotation>> additionalAnnotations)
    {
        List<Class<? extends Annotation>> annotations = Lists.newArrayList();
        annotations.add(AutoBindSingleton.class);
        annotations.add(Inject.class);
        annotations.add(javax.inject.Inject.class);
        annotations.add(Resource.class);
        annotations.add(Resources.class);
        if ( additionalAnnotations != null )
        {
            annotations.addAll(additionalAnnotations);
        }
        return new ClasspathScanner(basePackages, annotations);
    }

    /**
     * Return the internally created lifecycle manager
     *
     * @return manager
     */
    public LifecycleManager getLifecycleManager()
    {
        return lifecycleManager;
    }

    /**
     * Create an injector that is a child of the bootstrap bindings only
     *
     * @param modules binding modules
     * @return injector
     */
    public Injector createChildInjector(Module... modules)
    {
        return createChildInjector(Arrays.asList(modules));
    }

    /**
     * Create an injector that is a child of the bootstrap bindings only
     *
     * @param modules binding modules
     * @return injector
     */
    public Injector createChildInjector(Collection<Module> modules)
    {
        return injector.createChildInjector(modules);
    }

    /**
     * Create the main injector
     *
     * @return injector
     */
    public Injector createInjector()
    {
        return createInjector(Lists.<Module>newArrayList());
    }

    /**
     * Create the main injector
     *
     * @param modules any additional modules
     * @return injector
     */
    public Injector createInjector(Module... modules)
    {
        return createInjector(Arrays.asList(modules));
    }

    /**
     * Create the main injector
     *
     * @param additionalModules any additional modules
     * @return injector
     */
    public Injector createInjector(Collection<Module> additionalModules)
    {
        // Add the discovered modules FIRST.  The discovered modules
        // are added, and will subsequently be configured, in module dependency 
        // order which will ensure that any singletons bound in these modules 
        // will be created in the same order as the bind() calls are made.
        // Note that the singleton ordering is only guaranteed for 
        // singleton scope.
        List<Module> localModules = Lists.newArrayList(discoveredModules);
        
        if ( additionalModules != null )
        {
            localModules.addAll(additionalModules);
        }
        
        localModules.addAll(modules);

        // Finally, add the AutoBind module, which will use classpath scanning
        // to creating singleton bindings.  These singletons will be instantiated
        // in an indeterminate order but are guaranteed to occur AFTER singletons
        // bound in any of the discovered modules.
        if ( !ignoreAllClasses )
        {
            Collection<Class<?>>    localIgnoreClasses = Sets.newHashSet(ignoreClasses);
            localModules.add(new InternalAutoBindModule(injector, scanner, localIgnoreClasses));
        }

        return createChildInjector(localModules);
    }

    LifecycleInjector(List<Module> modules, Collection<Class<?>> ignoreClasses, boolean ignoreAllClasses, List<BootstrapModule> bootstrapModules, ClasspathScanner scanner, Collection<String> basePackages, Stage stage, Class<?> rootModule)
    {
        stage = Preconditions.checkNotNull(stage, "stage cannot be null");

        this.ignoreAllClasses = ignoreAllClasses;
        this.ignoreClasses = ImmutableList.copyOf(ignoreClasses);
        this.modules = ImmutableList.copyOf(modules);
        this.scanner = (scanner != null) ? scanner : createStandardClasspathScanner(basePackages);
        
        InternalModuleDependencyModule moduleDepdencyModule = new InternalModuleDependencyModule();
        AtomicReference<LifecycleManager> lifecycleManagerRef = new AtomicReference<LifecycleManager>();
        injector = Guice.createInjector
        (
            stage,
            new InternalBootstrapModule(this.scanner, bootstrapModules),
            new InternalLifecycleModule(lifecycleManagerRef),
            moduleDepdencyModule
        );
        if (rootModule != null)
            injector.getInstance(rootModule);
        this.discoveredModules.addAll(moduleDepdencyModule.getModules());
        lifecycleManager = injector.getInstance(LifecycleManager.class);
        lifecycleManagerRef.set(lifecycleManager);
    }
}
