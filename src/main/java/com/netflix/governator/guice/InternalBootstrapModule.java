package com.netflix.governator.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.annotations.RequiredAsset;
import com.netflix.governator.annotations.RequiredAssets;
import com.netflix.governator.assets.AssetLoader;
import com.netflix.governator.configuration.CompositeConfigurationProvider;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.configuration.PropertiesConfigurationProvider;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleManager;
import java.util.Properties;
import java.util.Set;

class InternalBootstrapModule extends AbstractModule
{
    private final ClasspathScanner scanner;
    private final BootstrapModule bootstrapModule;
    private final CompositeConfigurationProvider configurationProvider = new CompositeConfigurationProvider();

    private static final String DUMMY_NAME = "__" + InternalAutoBindModule.class.getName() + "__";
    private static final PropertiesConfigurationProvider dummyConfigurationProvider = new PropertiesConfigurationProvider(new Properties());

    public InternalBootstrapModule(ClasspathScanner scanner, BootstrapModule bootstrapModule)
    {
        this.scanner = scanner;
        this.bootstrapModule = bootstrapModule;
    }

    @Override
    protected void configure()
    {
        BootstrapBinder         bootstrapBinder = new BootstrapBinder(binder());

        // make some dummy bindings to get the maps created - this way users aren't required to have mappings
        bootstrapBinder.bindConfigurationProvider().toInstance(dummyConfigurationProvider);
        bootstrapBinder.bindAssetLoader(DUMMY_NAME).toInstance(new DummyAssetLoader());

        if ( bootstrapModule != null )
        {
            bootstrapModule.configure(bootstrapBinder);
        }

        bindLoaders(bootstrapBinder);
        binder().bind(LifecycleManager.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    public CompositeConfigurationProvider getCompositeConfigurationProvider(Set<ConfigurationProvider> configurationProviders)
    {
        for ( ConfigurationProvider provider : configurationProviders )
        {
            if ( provider != dummyConfigurationProvider )
            {
                configurationProvider.add(provider);
            }
        }
        return configurationProvider;
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

    private static class DummyAssetLoader implements AssetLoader
    {
        @Override
        public void loadAsset(String name) throws Exception
        {
        }

        @Override
        public void unloadAsset(String name) throws Exception
        {
        }
    }
}
