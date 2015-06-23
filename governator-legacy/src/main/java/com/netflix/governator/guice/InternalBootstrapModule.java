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
import java.util.Set;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.Stage;
import com.netflix.governator.configuration.ConfigurationDocumentation;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.guice.lazy.FineGrainedLazySingleton;
import com.netflix.governator.guice.lazy.FineGrainedLazySingletonScope;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.netflix.governator.guice.lazy.LazySingletonScope;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleConfigurationProviders;
import com.netflix.governator.lifecycle.LifecycleManager;

class InternalBootstrapModule extends AbstractModule
{
    private BootstrapBinder bootstrapBinder;
    private ClasspathScanner scanner;
    private Stage stage;
    private LifecycleInjectorMode mode;
    private ModuleListBuilder modules;
    private Collection<PostInjectorAction> actions;
    private Collection<ModuleTransformer> transformers;
    private boolean disableAutoBinding;
    private final Collection<BootstrapModule> bootstrapModules;
    
    private static class LifecycleConfigurationProvidersProvider implements Provider<LifecycleConfigurationProviders>
    {
        @Inject(optional = true)
        private Set<ConfigurationProvider> configurationProviders = Sets.newHashSet();

        @Override
        public LifecycleConfigurationProviders get()
        {
            return new LifecycleConfigurationProviders(configurationProviders);
        }
    }

    public InternalBootstrapModule(Collection<BootstrapModule> bootstrapModules, ClasspathScanner scanner, Stage stage, LifecycleInjectorMode mode, ModuleListBuilder modules, Collection<PostInjectorAction> actions, Collection<ModuleTransformer> transformers, boolean disableAutoBinding) {
        this.scanner = scanner;
        this.stage = stage;
        this.mode = mode;
        this.modules = modules;
        this.actions = actions;
        this.transformers = transformers;
        this.bootstrapModules = bootstrapModules;
        this.disableAutoBinding = disableAutoBinding;
    }

    BootstrapBinder getBootstrapBinder()
    {
        return bootstrapBinder;
    }

    @Override
    protected void configure()
    {
        bind(ConfigurationDocumentation.class).in(Scopes.SINGLETON);
        
        bindScope(LazySingleton.class, LazySingletonScope.get());
        bindScope(FineGrainedLazySingleton.class, FineGrainedLazySingletonScope.get());

        bootstrapBinder = new BootstrapBinder(binder(), stage, mode, modules, actions, transformers, disableAutoBinding);
        
        if ( bootstrapModules != null )
        {
            for (BootstrapModule bootstrapModule : bootstrapModules) {
                bootstrapModule.configure(bootstrapBinder);
            }
        }

        binder().bind(LifecycleManager.class).asEagerSingleton();
        binder().bind(LifecycleConfigurationProviders.class).toProvider(LifecycleConfigurationProvidersProvider.class).asEagerSingleton();
        
        this.stage = bootstrapBinder.getStage();
        this.mode = bootstrapBinder.getMode();
    }

    Stage getStage() {
        return stage;
    }
    
    LifecycleInjectorMode getMode() {
        return mode;
    }
    
    boolean isDisableAutoBinding() {
        return disableAutoBinding;
    }
    
    ModuleListBuilder getModuleListBuilder() {
        return modules;
    }
    
    @Provides
    @Singleton
    public ClasspathScanner getClasspathScanner()
    {
        return scanner;
    }
}
