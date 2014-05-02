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

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.netflix.governator.lifecycle.ClasspathScanner;

class LifecycleInjectorBuilderImpl implements LifecycleInjectorBuilder
{
    private List<Module> modules = Lists.newArrayList();
    private Collection<Class<?>> ignoreClasses = Lists.newArrayList();
    private Collection<String> basePackages = Lists.newArrayList();
    private boolean ignoreAllClasses = false;
    private List<BootstrapModule> bootstrapModules = Lists.newArrayList();
    private ClasspathScanner scanner = null;
    private Stage stage = Stage.PRODUCTION;
    @SuppressWarnings("deprecation")
    private LifecycleInjectorMode mode = LifecycleInjectorMode.REAL_CHILD_INJECTORS;
    private List<Class<? extends Module>> moduleClasses = ImmutableList.of();

    public LifecycleInjectorBuilder withBootstrapModule(BootstrapModule module)
    {
        this.bootstrapModules = ImmutableList.of(module);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withAdditionalBootstrapModules(BootstrapModule... additionalBootstrapModules) 
    {
        return withAdditionalBootstrapModules(ImmutableList.copyOf(additionalBootstrapModules));
    }

    @Override
    public LifecycleInjectorBuilder withAdditionalBootstrapModules(Iterable<? extends BootstrapModule> additionalBootstrapModules) 
    {
        this.bootstrapModules = ImmutableList.<BootstrapModule>builder()
                .addAll(this.bootstrapModules)
                .addAll(additionalBootstrapModules)
                .build();
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModules(Module... modules)
    {
        this.modules = ImmutableList.copyOf(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModules(Iterable<? extends Module> modules)
    {
        this.modules = ImmutableList.copyOf(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withAdditionalModules(Iterable<? extends Module> additionalModules)
    {
        this.modules = ImmutableList.<Module>builder()
                .addAll(this.modules)
                .addAll(additionalModules)
                .build();
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withAdditionalModules(Module... modules)
    {
        return withAdditionalModules(ImmutableList.copyOf(modules));
    }

    @Override
    public LifecycleInjectorBuilder withRootModule(Class<?> rootModule) 
    {
        return withModuleClass((Class<Module>) rootModule);
    }
    
    @Override
    public LifecycleInjectorBuilder withModuleClass(Class<? extends Module> module) 
    {
        this.moduleClasses = ImmutableList.<Class<? extends Module>>of(module);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModuleClasses(Iterable<Class<? extends Module>> modules) 
    {
        this.moduleClasses = ImmutableList.<Class<? extends Module>>copyOf(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModuleClasses(Class<? extends Module> ... modules) 
    {
        this.moduleClasses = ImmutableList.<Class<? extends Module>>copyOf(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withAdditionalModuleClasses(Iterable<Class<? extends Module>> modules) 
    {
        this.moduleClasses = ImmutableList.<Class<? extends Module>>builder()
                .addAll(this.moduleClasses)
                .addAll(modules)
                .build();
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withAdditionalModuleClasses(Class<? extends Module> ... modules) {
        this.moduleClasses = ImmutableList.<Class<? extends Module>>builder()
                .addAll(this.moduleClasses)
                .addAll(Lists.newArrayList(modules))
                .build();
        return this;
    }

    @Override
    public LifecycleInjectorBuilder ignoringAutoBindClasses(Collection<Class<?>> ignoreClasses)
    {
        this.ignoreClasses = ImmutableList.copyOf(ignoreClasses);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder ignoringAllAutoBindClasses()
    {
        this.ignoreAllClasses = true;
        return this;
    }

    @Override
    public LifecycleInjectorBuilder usingBasePackages(String... basePackages)
    {
        return usingBasePackages(Arrays.asList(basePackages));
    }

    @Override
    public LifecycleInjectorBuilder usingBasePackages(Collection<String> basePackages)
    {
        this.basePackages = Lists.newArrayList(basePackages);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder usingClasspathScanner(ClasspathScanner scanner)
    {
        this.scanner = scanner;
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withMode(LifecycleInjectorMode mode)
    {
        this.mode = mode;
        return this;
    }

    @Override
    public LifecycleInjectorBuilder inStage(Stage stage)
    {
        this.stage = stage;
        return this;
    }

    @Override
    public LifecycleInjector build()
    {
        return new LifecycleInjector(modules, ignoreClasses, ignoreAllClasses, bootstrapModules, scanner, basePackages, stage, mode, moduleClasses);
    }

    @Override
    @Deprecated
    public Injector createInjector()
    {
        return build().createInjector();
    }

    LifecycleInjectorBuilderImpl()
    {
    }
}
