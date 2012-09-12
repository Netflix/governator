package com.netflix.governator.guice;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.netflix.governator.guice.lazy.LazySingletonScope;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleConfigurationProviders;
import com.netflix.governator.lifecycle.LifecycleManager;
import java.util.Set;

class InternalBootstrapModule extends AbstractModule
{
    private final ClasspathScanner scanner;
    private final BootstrapModule bootstrapModule;

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

    InternalBootstrapModule(ClasspathScanner scanner, BootstrapModule bootstrapModule)
    {
        this.scanner = scanner;
        this.bootstrapModule = bootstrapModule;
    }

    @Override
    protected void configure()
    {
        bindScope(LazySingleton.class, LazySingletonScope.get());

        BootstrapBinder         bootstrapBinder = new BootstrapBinder(binder());

        if ( bootstrapModule != null )
        {
            bootstrapModule.configure(bootstrapBinder);
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
        for ( Class<?> clazz : scanner.get() )
        {
            if ( clazz.isAnnotationPresent(AutoBindSingleton.class) && ConfigurationProvider.class.isAssignableFrom(clazz) )
            {
                @SuppressWarnings("unchecked")
                Class<? extends ConfigurationProvider>    configurationProviderClass = (Class<? extends ConfigurationProvider>)clazz;
                binder.bindConfigurationProvider().to(configurationProviderClass).asEagerSingleton();
            }
        }
    }
}
