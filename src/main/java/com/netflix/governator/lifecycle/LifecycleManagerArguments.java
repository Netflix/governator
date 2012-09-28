package com.netflix.governator.lifecycle;

import com.google.common.collect.ImmutableSet;
import com.google.inject.Inject;
import java.util.Collection;
import java.util.Set;

public class LifecycleManagerArguments
{
    @Inject(optional = true)
    private LifecycleConfigurationProviders configurationProvider = new LifecycleConfigurationProviders();

    @Inject(optional = true)
    private Set<LifecycleListener> lifecycleListeners = ImmutableSet.of();

    @Inject
    public LifecycleManagerArguments()
    {
    }

    public LifecycleConfigurationProviders getConfigurationProvider()
    {
        return configurationProvider;
    }

    public Collection<LifecycleListener> getLifecycleListeners()
    {
        return lifecycleListeners;
    }

    public void setConfigurationProvider(LifecycleConfigurationProviders configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }

    public void setLifecycleListeners(Collection<LifecycleListener> lifecycleListeners)
    {
        this.lifecycleListeners = ImmutableSet.copyOf(lifecycleListeners);
    }
}
