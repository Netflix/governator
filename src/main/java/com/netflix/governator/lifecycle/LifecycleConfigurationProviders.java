package com.netflix.governator.lifecycle;

import com.netflix.governator.configuration.CompositeConfigurationProvider;
import com.netflix.governator.configuration.ConfigurationProvider;
import java.util.Collection;

public class LifecycleConfigurationProviders extends CompositeConfigurationProvider
{
    public LifecycleConfigurationProviders(ConfigurationProvider... providers)
    {
        super(providers);
    }

    public LifecycleConfigurationProviders(Collection<ConfigurationProvider> providers)
    {
        super(providers);
    }
}
