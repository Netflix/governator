package com.netflix.governator.assets;

public interface RequiredAssetParametersView
{
    public<T> T        get(Class<T> parameterClass);

    public<T> T        get(GenericParameterType<T> parameterType);
}
