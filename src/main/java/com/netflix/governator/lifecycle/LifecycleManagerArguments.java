package com.netflix.governator.lifecycle;

import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.netflix.governator.assets.AssetLoader;
import java.util.Map;

class LifecycleManagerArguments
{
    @Inject(optional = true)
    Map<String, AssetLoader> assetLoaders = Maps.newHashMap();

    @Inject(optional = true)
    LifecycleConfigurationProviders configurationProvider = new LifecycleConfigurationProviders();
}
