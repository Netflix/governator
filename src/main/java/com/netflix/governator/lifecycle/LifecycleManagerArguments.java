package com.netflix.governator.lifecycle;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.netflix.governator.assets.AssetLoader;
import java.util.Map;

public class LifecycleManagerArguments
{
    @Inject(optional = true)
    private Map<String, AssetLoader> assetLoaders = Maps.newHashMap();

    @Inject(optional = true)
    private LifecycleConfigurationProviders configurationProvider = new LifecycleConfigurationProviders();

    @Inject(optional = true)
    private AssetLoader defaultAssetLoader = null;

    @Inject
    public LifecycleManagerArguments()
    {
    }

    public Map<String, AssetLoader> getAssetLoaders()
    {
        return assetLoaders;
    }

    public LifecycleConfigurationProviders getConfigurationProvider()
    {
        return configurationProvider;
    }

    public AssetLoader getDefaultAssetLoader()
    {
        return defaultAssetLoader;
    }

    public void setDefaultAssetLoader(AssetLoader defaultAssetLoader)
    {
        this.defaultAssetLoader = defaultAssetLoader;
    }
}
