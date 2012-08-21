package com.netflix.governator.guice;

import com.google.inject.Provider;
import com.netflix.governator.configuration.ConfigurationProvider;

/**
 * Binding builder for {@link ConfigurationProvider}
 */
public interface AssetParameterBuilder<T>
{
    public void         toValue(T value);
}
