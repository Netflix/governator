package com.netflix.governator.guice.mocks;

import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.RequiredAsset;
import com.netflix.governator.assets.PropertyFileAssetLoader;

@RequiredAsset(value = "test.properties", loader = PropertyFileAssetLoader.class)
public class AssetLoadingMock
{
    @Configuration("test.property.int")
    public int         anInt;

    @Configuration("test.property.double")
    public double      aDouble;

    @Configuration("test.property.string")
    public String      aString;

    @Configuration("not-present-in-file")
    public String      shouldBeDefault = "default";
}
