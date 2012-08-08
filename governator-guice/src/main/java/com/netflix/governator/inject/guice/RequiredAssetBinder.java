package com.netflix.governator.inject.guice;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.netflix.governator.lifecycle.AssetLoader;

public class RequiredAssetBinder
{
    private final Binder binder;

    public static LinkedBindingBuilder<AssetLoader> bindRequiredAsset(Binder binder, String requiredAssetValue)
    {
        requiredAssetValue = Preconditions.checkNotNull(requiredAssetValue, "requiredAssetValue cannot be null");
        MapBinder<String, AssetLoader>  mapBinder = MapBinder.newMapBinder(binder, String.class, AssetLoader.class);
        return mapBinder.addBinding(requiredAssetValue);
    }

    public LinkedBindingBuilder<AssetLoader> bindRequiredAsset(String requiredAssetValue)
    {
        return bindRequiredAsset(binder, requiredAssetValue);
    }

    RequiredAssetBinder(Binder binder)
    {
        this.binder = binder;
    }
}
