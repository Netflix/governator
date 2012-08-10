package com.netflix.governator.assets;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import javax.inject.Inject;
import java.util.Map;

public class RequiredAssetParameters
{
    private final Map<Object, Map<String, Object>>  parameters = Maps.newHashMap();

    @Inject
    public RequiredAssetParameters()
    {
    }

    public<T> void     set(String requiredAsset, GenericParameterType<T> key, T parameter)
    {
        internalSet(requiredAsset, key.getType(), parameter);
    }

    public<T> void     set(String requiredAsset, Class<T> key, T parameter)
    {
        internalSet(requiredAsset, key, parameter);
    }

    public void        internalSet(String requiredAsset, Object key, Object parameter)
    {
        requiredAsset = Preconditions.checkNotNull(requiredAsset, "requiredAsset cannot be null");
        key = Preconditions.checkNotNull(key, "key cannot be null");
        parameter = Preconditions.checkNotNull(parameter, "parameter cannot be null");

        Map<String, Object> values = parameters.get(key);
        if ( values == null )
        {
            values = Maps.newHashMap();
            parameters.put(key, values);
        }

        Preconditions.checkState(!values.containsKey(requiredAsset), String.format("A parameter has already been set for that requiredAsset (%s) and parameter key (%s)", requiredAsset, key.getClass().getName()));
        values.put(requiredAsset, parameter);
    }

    public<T> T        get(String requiredAsset, GenericParameterType<T> key)
    {
        Object      value = internalGet(requiredAsset, key.getType());
        //noinspection unchecked
        return (T)value;
    }

    public<T> T        get(String requiredAsset, Class<T> key)
    {
        Object      value = internalGet(requiredAsset, key);
        return key.cast(value);
    }

    private Object        internalGet(String requiredAsset, Object key)
    {
        requiredAsset = Preconditions.checkNotNull(requiredAsset, "requiredAsset cannot be null");
        key = Preconditions.checkNotNull(key, "key cannot be null");

        Map<String, Object>     values = parameters.get(key);
        if ( values != null )
        {
            return values.get(requiredAsset);
        }
        return null;
    }

    public RequiredAssetParametersView  getView(final String requiredAsset)
    {
        return new RequiredAssetParametersView()
        {
            @Override
            public <T> T get(Class<T> parameterClass)
            {
                return RequiredAssetParameters.this.get(requiredAsset, parameterClass);
            }

            @Override
            public <T> T get(GenericParameterType<T> parameterType)
            {
                return RequiredAssetParameters.this.get(requiredAsset, parameterType);
            }
        };
    }
}
