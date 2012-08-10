package com.netflix.governator.assets;

public interface AssetParametersView
{
    public<T> T        get(Class<T> parameterClass);

    public<T> T        get(GenericParameterType<T> parameterType);
}
