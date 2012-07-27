package com.netflix.governator.inject.guice;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.Multibinder;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.lifecycle.AssetLoader;

public class LifecycleBinder
{
    private final Binder binder;

    public LinkedBindingBuilder<AssetLoader> bindAssetLoader()
    {
        return Multibinder.newSetBinder(binder, AssetLoader.class).addBinding();
    }

    public <T extends ConfigurationProvider> LinkedBindingBuilder<T> bindConfigurationProvider(Key<T> key)
    {
        return binder.bind(key);
    }

    public <T extends ConfigurationProvider> AnnotatedBindingBuilder<T> bindConfigurationProvider(TypeLiteral<T> typeLiteral)
    {
        return binder.bind(typeLiteral);
    }

    public AnnotatedBindingBuilder<? extends ConfigurationProvider> bindConfigurationProvider()
    {
        return binder.bind(ConfigurationProvider.class);
    }

    LifecycleBinder(Binder binder)
    {
        this.binder = binder;
    }
}
