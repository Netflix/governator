package com.netflix.governator.guice;

import com.google.common.base.Preconditions;
import com.google.inject.Binder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.multibindings.MapBinder;
import com.netflix.governator.assets.AssetLoader;
import com.netflix.governator.assets.AssetParameters;
import com.netflix.governator.assets.AssetParametersView;
import com.netflix.governator.assets.GenericParameterType;

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
    public static LinkedBindingBuilder<AssetParametersView> bindRequiredAssetParameters(Binder binder, String requiredAssetValue)
    {
        requiredAssetValue = Preconditions.checkNotNull(requiredAssetValue, "requiredAssetValue cannot be null");
        MapBinder<String, AssetParametersView>  mapBinder = MapBinder.newMapBinder(binder, String.class, AssetParametersView.class);
        return mapBinder.addBinding(requiredAssetValue);
    }

    /**
     * Convenience method to set a single parameter value for an asset
     *
     * @param binder Guice binder
     * @param requiredAssetValue asset name/value
     * @param key key for the parameter
     * @param value parameter value
     */
    public static<T> void setRequiredAssetParameter(Binder binder, String requiredAssetValue, Class<T> key, T value)
    {
        requiredAssetValue = Preconditions.checkNotNull(requiredAssetValue, "requiredAssetValue cannot be null");
        key = Preconditions.checkNotNull(key, "key cannot be null");
        value = Preconditions.checkNotNull(value, "value cannot be null");

        AssetParameters parameters = new AssetParameters();
        parameters.set(key, value);
        bindRequiredAssetParameters(binder, requiredAssetValue).toInstance(parameters);
    }

    /**
     * Convenience method to set a single parameter value for an asset
     *
     * @param binder Guice binder
     * @param requiredAssetValue asset name/value
     * @param key key for the parameter
     * @param value parameter value
     */
    public static<T> void setRequiredAssetParameter(Binder binder, String requiredAssetValue, GenericParameterType<T> key, T value)
    {
        requiredAssetValue = Preconditions.checkNotNull(requiredAssetValue, "requiredAssetValue cannot be null");
        key = Preconditions.checkNotNull(key, "key cannot be null");
        value = Preconditions.checkNotNull(value, "value cannot be null");

        AssetParameters parameters = new AssetParameters();
        parameters.set(key, value);
        bindRequiredAssetParameters(binder, requiredAssetValue).toInstance(parameters);
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
    public LinkedBindingBuilder<AssetParametersView> bindRequiredAssetParameters(String requiredAssetValue)
    {
        return bindRequiredAssetParameters(binder, requiredAssetValue);
    }

    /**
     * Convenience method to set a single parameter value for an asset
     *
     * @param requiredAssetValue asset name/value
     * @param key key for the parameter
     * @param value parameter value
     */
    public<T> void setRequiredAssetParameter(String requiredAssetValue, Class<T> key, T value)
    {
        setRequiredAssetParameter(binder, requiredAssetValue, key, value);
    }

    /**
     * Convenience method to set a single parameter value for an asset
     *
     * @param requiredAssetValue asset name/value
     * @param key key for the parameter
     * @param value parameter value
     */
    public<T> void setRequiredAssetParameter(String requiredAssetValue, GenericParameterType<T> key, T value)
    {
        setRequiredAssetParameter(binder, requiredAssetValue, key, value);
    }

    RequiredAssetBinder(Binder binder)
    {
        this.binder = binder;
    }
}
