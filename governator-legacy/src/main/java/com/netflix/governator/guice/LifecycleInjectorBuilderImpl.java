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

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.netflix.governator.lifecycle.ClasspathScanner;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

class LifecycleInjectorBuilderImpl implements LifecycleInjectorBuilder
{
    private ModuleListBuilder modules = new ModuleListBuilder();
    private Collection<Class<?>> ignoreClasses = Lists.newArrayList();
    private Collection<String> basePackages = Lists.newArrayList();
    private boolean disableAutoBinding = false;
    private List<BootstrapModule> bootstrapModules = Lists.newArrayList();
    private ClasspathScanner scanner = null;
    private Stage stage = Stage.PRODUCTION;
    @SuppressWarnings("deprecation")
    private LifecycleInjectorMode lifecycleInjectorMode = LifecycleInjectorMode.REAL_CHILD_INJECTORS;
    private List<PostInjectorAction> actions = ImmutableList.of();
    private List<ModuleTransformer> transformers = ImmutableList.of();

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
        if (additionalBootstrapModules != null) 
        {
            this.bootstrapModules = ImmutableList.<BootstrapModule>builder()
                    .addAll(this.bootstrapModules)
                    .addAll(additionalBootstrapModules)
                    .build();
        }
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModules(Module... modules)
    {
        this.modules = new ModuleListBuilder().includeModules(ImmutableList.copyOf(modules));
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModules(Iterable<? extends Module> modules)
    {
        if (modules != null) 
        {
            this.modules = new ModuleListBuilder().includeModules(modules);
        }
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withAdditionalModules(Iterable<? extends Module> additionalModules)
    {
        if (additionalModules != null) {
            try {
                this.modules.includeModules(additionalModules);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
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
        if (rootModule == null)
            return this;
        return withModuleClass((Class<? extends Module>) rootModule);
    }
    
    @Override
    public LifecycleInjectorBuilder withModuleClass(Class<? extends Module> module) 
    {
        if (module != null) {
            this.modules.include(module);
        }
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModuleClasses(Iterable<Class<? extends Module>> modules) 
    {
        if (modules != null) {
            this.modules.include(modules);
        }
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModuleClasses(Class<?> ... modules) 
    {
        this.modules.include(ImmutableList.<Class<? extends Module>>copyOf(
            Iterables.transform(Lists.newArrayList(modules), new Function<Class<?>, Class<? extends Module>>() {
                @SuppressWarnings("unchecked")
                @Override
                public Class<? extends Module> apply(Class<?> input) {
                    return (Class<? extends Module>) input;
                }
            }
        )));
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withAdditionalModuleClasses(Iterable<Class<? extends Module>> modules) 
    {
        this.modules.include(modules);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withAdditionalModuleClasses(Class<?> ... modules) 
    {
        this.modules.include(ImmutableList.<Class<? extends Module>>builder()
                .addAll(Iterables.transform(Lists.newArrayList(modules), new Function<Class<?>, Class<? extends Module>>() {
                    @SuppressWarnings("unchecked")
                    @Override
                    public Class<? extends Module> apply(Class<?> input) {
                        return (Class<? extends Module>) input;
                    }
                }))
                .build());
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withoutModuleClasses(Class<? extends Module> ... modules) 
    {
        this.modules.exclude(ImmutableList.copyOf(modules));
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withoutModuleClass(Class<? extends Module> module) 
    {
        this.modules.exclude(module);
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withoutModuleClasses(Iterable<Class<? extends Module>> modules) 
    {
        this.modules.exclude(modules);
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
        this.disableAutoBinding = true;
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
        this.lifecycleInjectorMode = mode;
        return this;
    }

    @Override
    public LifecycleInjectorBuilder inStage(Stage stage)
    {
        this.stage = stage;
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModuleTransformer(ModuleTransformer filter) {
        if (filter != null) {
            this.transformers = ImmutableList.<ModuleTransformer>builder()
                .addAll(this.transformers)
                .add(filter)
                .build();
        }
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModuleTransformer(Collection<? extends ModuleTransformer> filters) {
        if (this.transformers != null) {
            this.transformers = ImmutableList.<ModuleTransformer>builder()
                .addAll(this.transformers)
                .addAll(filters)
                .build();
        }
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withModuleTransformer(ModuleTransformer... filters) {
        if (this.transformers != null) {
            this.transformers = ImmutableList.<ModuleTransformer>builder()
                .addAll(this.transformers)
                .addAll(ImmutableList.copyOf(filters))
                .build();
        }
        return this;
    }

    @Override
    public LifecycleInjectorBuilder withPostInjectorAction(PostInjectorAction action) {
        this.actions = ImmutableList.<PostInjectorAction>builder()
            .addAll(this.actions)
            .add(action)
            .build();
        return this;
    }
    
    @Override
    public LifecycleInjectorBuilder withPostInjectorActions(Collection<? extends PostInjectorAction> actions) {
        if (actions != null) {
            this.actions = ImmutableList.<PostInjectorAction>builder()
                .addAll(this.actions)
                .addAll(actions)
                .build();
        }
        return this;
    }
    
    @Override
    public LifecycleInjectorBuilder withPostInjectorActions(PostInjectorAction... actions) {
        this.actions = ImmutableList.<PostInjectorAction>builder()
            .addAll(this.actions)
            .addAll(ImmutableList.copyOf(actions))
            .build();
        return this;
    }
    
    @Override
    public LifecycleInjector build()
    {
        try {
            return new LifecycleInjector(this);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
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
    
    ModuleListBuilder getModuleListBuilder() {
        return modules;
    }
    
    Collection<Class<?>> getIgnoreClasses() {
        return ignoreClasses;
    }
    
    Collection<String> getBasePackages() {
        return basePackages;
    }
    
    List<BootstrapModule> getBootstrapModules() {
        return bootstrapModules;
    }
    
    ClasspathScanner getClasspathScanner() {
        return scanner;
    }
    
    Stage getStage() {
        return stage;
    }
    
    LifecycleInjectorMode getLifecycleInjectorMode() {
        return lifecycleInjectorMode;
    }
    
    List<PostInjectorAction> getPostInjectorActions() {
        return actions;
    }
    
    List<ModuleTransformer> getModuleTransformers() {
        return transformers;
    }

    boolean isDisableAutoBinding() {
        return this.disableAutoBinding;
    }

}
