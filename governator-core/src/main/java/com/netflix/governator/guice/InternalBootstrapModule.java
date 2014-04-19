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
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.configuration.ConfigurationDocumentation;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.guice.lazy.FineGrainedLazySingleton;
import com.netflix.governator.guice.lazy.FineGrainedLazySingletonScope;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.netflix.governator.guice.lazy.LazySingletonScope;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleConfigurationProviders;
import com.netflix.governator.lifecycle.LifecycleManager;
import java.util.List;
import java.util.Set;

class InternalBootstrapModule extends AbstractModule
{
    private final ClasspathScanner scanner;
    private BootstrapBinder bootstrapBinder;
    private final List<BootstrapModule> bootstrapModules;

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

    InternalBootstrapModule(ClasspathScanner scanner, List<BootstrapModule> bootstrapModules)
    {
        this.scanner = scanner;
        this.bootstrapModules = ImmutableList.copyOf(bootstrapModules);
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

        bootstrapBinder = new BootstrapBinder(binder());

        if ( bootstrapModules != null )
        {
            for (BootstrapModule bootstrapModule : bootstrapModules) {
                bootstrapModule.configure(bootstrapBinder);
            }
        }

        bindLoaders(bootstrapBinder);
        binder().bind(LifecycleManager.class).asEagerSingleton();
        binder().bind(LifecycleConfigurationProviders.class).toProvider(LifecycleConfigurationProvidersProvider.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    public ClasspathScanner getClasspathScanner()
    {
        return scanner;
    }

    private void bindLoaders(BootstrapBinder binder)
    {
        for ( Class<?> clazz : scanner.getClasses() )
        {
            if ( clazz.isAnnotationPresent(AutoBindSingleton.class) && ConfigurationProvider.class.isAssignableFrom(clazz) )
            {
                AutoBindSingleton annotation = clazz.getAnnotation(AutoBindSingleton.class);
                Preconditions.checkState(annotation.value() == AutoBindSingleton.class, "@AutoBindSingleton value cannot be set for ConfigurationProviders");
                Preconditions.checkState(annotation.baseClass() == AutoBindSingleton.class, "@AutoBindSingleton value cannot be set for ConfigurationProviders");
                Preconditions.checkState(!annotation.multiple(), "@AutoBindSingleton(multiple=true) value cannot be set for ConfigurationProviders");

                @SuppressWarnings("unchecked")
                Class<? extends ConfigurationProvider>    configurationProviderClass = (Class<? extends ConfigurationProvider>)clazz;
                binder.bindConfigurationProvider().to(configurationProviderClass).asEagerSingleton();
            }
        }
    }
}
