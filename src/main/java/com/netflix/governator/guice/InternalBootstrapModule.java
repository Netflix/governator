package com.netflix.governator.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.annotations.RequiredAsset;
import com.netflix.governator.annotations.RequiredAssets;
import com.netflix.governator.assets.AssetLoader;
import com.netflix.governator.assets.AssetParameters;
import com.netflix.governator.assets.AssetParametersView;
import com.netflix.governator.configuration.CompositeConfigurationProvider;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.lifecycle.ClasspathScanner;
import com.netflix.governator.lifecycle.LifecycleManager;
import java.util.List;

class InternalBootstrapModule extends AbstractModule
{
    private final ClasspathScanner scanner;
    private final BootstrapModule bootstrapModule;
    private final CompositeConfigurationProvider configurationProvider = new CompositeConfigurationProvider();

    private static final String DUMMY_NAME = "__" + InternalAutoBindModule.class.getName() + "__";

    public InternalBootstrapModule(ClasspathScanner scanner, BootstrapModule bootstrapModule)
    {
        this.scanner = scanner;
        this.bootstrapModule = bootstrapModule;
    }

    @Override
    protected void configure()
    {
        // make some dummy bindings to get the maps created - this way users aren't required to have mappings
        RequiredAssetBinder.bindRequiredAsset(binder(), DUMMY_NAME).toInstance(new DummyAssetLoader());
        RequiredAssetBinder.bindRequiredAssetParameters(binder(), DUMMY_NAME).toInstance(new AssetParameters());

        BootstrapBinder         bootstrapBinder = new BootstrapBinder(binder());
        if ( bootstrapModule != null )
        {
            bootstrapModule.configure(bootstrapBinder);
        }

        bindLoaders(binder());
        binder().bind(new TypeLiteral<List<ConfigurationProviderBinding>>(){}).toInstance(bootstrapBinder.getConfigurationProviderBindings());
        binder().bind(LifecycleManager.class).asEagerSingleton();
    }

    @Provides
    @Singleton
    public CompositeConfigurationProvider getCompositeConfigurationProvider(Injector injector, List<ConfigurationProviderBinding> configurationProviderBindings)
    {
        for ( ConfigurationProviderBinding binding : configurationProviderBindings )
        {
            if ( binding.getClazz() != null )
            {
                configurationProvider.add(injector.getInstance(binding.getClazz()));
            }
            else if ( binding.getInstance() != null )
            {
                configurationProvider.add(binding.getInstance());
            }
            else if ( binding.getProvider() != null )
            {
                configurationProvider.add(binding.getProvider().get());
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
                    binder.bind(ConfigurationProvider.class).to(configurationProviderClass).asEagerSingleton();
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

    private static class DummyAssetLoader implements AssetLoader
    {
        @Override
        public void loadAsset(String name, AssetParametersView parameters) throws Exception
        {
        }

        @Override
        public void unloadAsset(String name, AssetParametersView parameters) throws Exception
        {
        }
    }
}
