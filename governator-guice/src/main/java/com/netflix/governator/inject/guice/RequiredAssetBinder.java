package com.netflix.governator.inject.guice;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.netflix.governator.assets.AssetLoader;
import com.netflix.governator.assets.AssetParameters;

public class RequiredAssetBinder
{
    private final Binder binder;

    public static LinkedBindingBuilder<AssetLoader> bindRequiredAsset(Binder binder, String requiredAssetValue)
    {
        requiredAssetValue = Preconditions.checkNotNull(requiredAssetValue, "requiredAssetValue cannot be null");
        MapBinder<String, AssetLoader>  mapBinder = MapBinder.newMapBinder(binder, String.class, AssetLoader.class);
        return mapBinder.addBinding(requiredAssetValue);
    }

    public static LinkedBindingBuilder<AssetParameters> bindRequestAssetParameters(Binder binder, String requiredAssetValue)
    {
        requiredAssetValue = Preconditions.checkNotNull(requiredAssetValue, "requiredAssetValue cannot be null");
        MapBinder<String, AssetParameters>  mapBinder = MapBinder.newMapBinder(binder, String.class, AssetParameters.class);
        return mapBinder.addBinding(requiredAssetValue);
    }

    public LinkedBindingBuilder<AssetLoader> bindRequiredAsset(String requiredAssetValue)
    {
        return bindRequiredAsset(binder, requiredAssetValue);
    }

    public LinkedBindingBuilder<AssetParameters> bindRequestAssetParameters(String requiredAssetValue)
    {
        return bindRequestAssetParameters(binder, requiredAssetValue);
    }

    RequiredAssetBinder(Binder binder)
    {
        this.binder = binder;
    }
}
