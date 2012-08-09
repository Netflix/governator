package com.netflix.governator.inject.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.annotations.RequiredAsset;
import com.netflix.governator.annotations.RequiredAssets;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.lifecycle.AssetLoader;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleManager;

class InternalBootstrapModule extends AbstractModule
{
    private final ClasspathScanner scanner;
    private final BootstrapModule bootstrapModule;

    public InternalBootstrapModule(ClasspathScanner scanner, BootstrapModule bootstrapModule)
    {
        this.scanner = scanner;
        this.bootstrapModule = bootstrapModule;
    }

    @Override
    protected void configure()
    {
        if ( bootstrapModule != null )
        {
            bootstrapModule.configure(binder(), new RequiredAssetBinder(binder()));
        }

        bindLoaders(binder());
        binder().bind(LifecycleManager.class).asEagerSingleton();
    }

    private void bindLoaders(Binder binder)
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
                    RequiredAssetBinder.bindRequiredAsset(binder, LifecycleManager.DEFAULT_ASSET_LOADER_VALUE).to(assetLoaderClass);
                }
                else if ( ConfigurationProvider.class.isAssignableFrom(clazz) )
                {
                    @SuppressWarnings("unchecked")
                    Class<? extends ConfigurationProvider>    configurationProviderClass = (Class<? extends ConfigurationProvider>)clazz;
                    binder.bind(ConfigurationProvider.class).to(configurationProviderClass);
                }
            }
        }
    }

    private void bindRequiredAsset(Binder binder, RequiredAsset requiredAsset)
    {
        if ( requiredAsset.loader() != AssetLoader.class )
        {
            RequiredAssetBinder.bindRequiredAsset(binder, requiredAsset.value()).to(requiredAsset.loader());
        }
    }
}
