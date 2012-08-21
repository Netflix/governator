package com.netflix.governator.guice;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.annotations.RequiredAsset;
import com.netflix.governator.annotations.RequiredAssets;
import com.netflix.governator.assets.AssetLoader;
import com.netflix.governator.configuration.ConfigurationProvider;
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

    public InternalBootstrapModule(ClasspathScanner scanner, BootstrapModule bootstrapModule)
    {
        this.scanner = scanner;
        this.bootstrapModule = bootstrapModule;
    }

    @Override
    protected void configure()
    {
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
            if ( clazz.isAnnotationPresent(RequiredAsset.class) )
            {
                RequiredAsset       requiredAsset = clazz.getAnnotation(RequiredAsset.class);
                bindRequiredAsset(binder, requiredAsset);
            }
            else if ( clazz.isAnnotationPresent(RequiredAssets.class) )
            {
                RequiredAssets       requiredAssets = clazz.getAnnotation(RequiredAssets.class);
                for ( RequiredAsset requiredAsset : requiredAssets.value() )
                {
                    bindRequiredAsset(binder, requiredAsset);
                }
            }

            if ( clazz.isAnnotationPresent(AutoBindSingleton.class) )
            {
                if ( AssetLoader.class.isAssignableFrom(clazz) )
                {
                    @SuppressWarnings("unchecked")
                    Class<? extends AssetLoader>    assetLoaderClass = (Class<? extends AssetLoader>)clazz;
                    binder.bindAssetLoader(LifecycleManager.DEFAULT_ASSET_LOADER_VALUE).to(assetLoaderClass);
                }
                else if ( ConfigurationProvider.class.isAssignableFrom(clazz) )
                {
                    @SuppressWarnings("unchecked")
                    Class<? extends ConfigurationProvider>    configurationProviderClass = (Class<? extends ConfigurationProvider>)clazz;
                    binder.bindConfigurationProvider().to(configurationProviderClass).asEagerSingleton();
                }
            }
        }
    }

    private void bindRequiredAsset(BootstrapBinder binder, RequiredAsset requiredAsset)
    {
        if ( requiredAsset.loader() != AssetLoader.class )
        {
            binder.bindAssetLoader(requiredAsset.value()).to(requiredAsset.loader());
        }
    }
}