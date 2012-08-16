package com.netflix.governator.guice;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.netflix.governator.assets.AssetLoader;
import com.netflix.governator.assets.AssetParametersView;

/**
 * Used to bind required assets and parameters.
 */
public class RequiredAssetBinder
{
    private final Binder binder;

    /**
     * Begin binding a required asset name/value to a loader
     *
     * @param binder Guice binder
     * @param requiredAssetValue asset name/value
     * @return binder
     */
    public static LinkedBindingBuilder<AssetLoader> bindRequiredAsset(Binder binder, String requiredAssetValue)
    {
        requiredAssetValue = Preconditions.checkNotNull(requiredAssetValue, "requiredAssetValue cannot be null");
        MapBinder<String, AssetLoader>  mapBinder = MapBinder.newMapBinder(binder, String.class, AssetLoader.class);
        return mapBinder.addBinding(requiredAssetValue);
    }

    /**
     * Begin binding a required asset name/value to an asset parameter
     *
     * @param binder Guice binder
     * @param requiredAssetValue asset name/value
     * @return binder
     */
    public static LinkedBindingBuilder<AssetParametersView> bindRequestAssetParameters(Binder binder, String requiredAssetValue)
    {
        requiredAssetValue = Preconditions.checkNotNull(requiredAssetValue, "requiredAssetValue cannot be null");
        MapBinder<String, AssetParametersView>  mapBinder = MapBinder.newMapBinder(binder, String.class, AssetParametersView.class);
        return mapBinder.addBinding(requiredAssetValue);
    }

    /**
     * Begin binding a required asset name/value to a loader
     *
     * @param requiredAssetValue asset name/value
     * @return binder
     */
    public LinkedBindingBuilder<AssetLoader> bindRequiredAsset(String requiredAssetValue)
    {
        return bindRequiredAsset(binder, requiredAssetValue);
    }

    /**
     * Begin binding a required asset name/value to an asset parameter
     *
     * @param requiredAssetValue asset name/value
     * @return binder
     */
    public LinkedBindingBuilder<AssetParametersView> bindRequestAssetParameters(String requiredAssetValue)
    {
        return bindRequestAssetParameters(binder, requiredAssetValue);
    }

    RequiredAssetBinder(Binder binder)
    {
        this.binder = binder;
    }
}
