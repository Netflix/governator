package com.netflix.governator.guice;

import com.google.inject.Provider;
import com.netflix.governator.configuration.ConfigurationProvider;

public interface ConfigurationProviderBuilder
{
    public void to(Class<? extends ConfigurationProvider> implementation);

    public void toInstance(ConfigurationProvider implementation);

    public void toProvider(Provider<? extends ConfigurationProvider> implementation);
}
