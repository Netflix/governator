package com.netflix.governator.guice;

import com.google.inject.Provider;
import com.netflix.governator.configuration.ConfigurationProvider;

public class ConfigurationProviderBinding
{
    private final Class<? extends ConfigurationProvider> clazz;
    private final ConfigurationProvider instance;
    private final Provider<? extends ConfigurationProvider> provider;

    public ConfigurationProviderBinding(Class<? extends ConfigurationProvider> clazz, ConfigurationProvider instance, Provider<? extends ConfigurationProvider> provider)
    {
        this.clazz = clazz;
        this.instance = instance;
        this.provider = provider;
    }

    public Class<? extends ConfigurationProvider> getClazz()
    {
        return clazz;
    }

    public ConfigurationProvider getInstance()
    {
        return instance;
    }

    public Provider<? extends ConfigurationProvider> getProvider()
    {
        return provider;
    }
}
