package com.netflix.governator.lifecycle;

import com.google.inject.Inject;

public class LifecycleManagerArguments
{
    @Inject(optional = true)
    private LifecycleConfigurationProviders configurationProvider = new LifecycleConfigurationProviders();

    @Inject(optional = true)
    private LifecycleListener lifecycleListener = null;

    @Inject
    public LifecycleManagerArguments()
    {
    }

    public LifecycleConfigurationProviders getConfigurationProvider()
    {
        return configurationProvider;
    }

    public LifecycleListener getLifecycleListener()
    {
        return lifecycleListener;
    }

    public void setConfigurationProvider(LifecycleConfigurationProviders configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }

    public void setLifecycleListener(LifecycleListener lifecycleListener)
    {
        this.lifecycleListener = lifecycleListener;
    }
}
