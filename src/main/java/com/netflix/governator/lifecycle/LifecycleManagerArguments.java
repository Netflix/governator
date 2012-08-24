package com.netflix.governator.lifecycle;

import com.google.inject.Inject;

public class LifecycleManagerArguments
{
    @Inject(optional = true)
    private LifecycleConfigurationProviders configurationProvider = new LifecycleConfigurationProviders();

    @Inject
    public LifecycleManagerArguments()
    {
    }

    public LifecycleConfigurationProviders getConfigurationProvider()
    {
        return configurationProvider;
    }
}
