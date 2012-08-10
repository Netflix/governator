package com.netflix.governator.lifecycle.mocks;

import com.netflix.governator.assets.AssetLoader;
import com.netflix.governator.assets.GenericParameterType;
import com.netflix.governator.assets.RequiredAssetParametersView;
import org.testng.Assert;
import java.util.Map;

public class ParameterizedAssetLoader implements AssetLoader
{
    @Override
    public void loadAsset(String name, RequiredAssetParametersView parameters) throws Exception
    {
        Map<String, String>                 map = parameters.get(new GenericParameterType<Map<String, String>>(){});
        Assert.assertNotNull(map);
        Assert.assertEquals(map.get("one"), "1");
        Assert.assertEquals(map.get("two"), "2");
        Assert.assertEquals(map.get("three"), "3");
    }

    @Override
    public void unloadAsset(String name, RequiredAssetParametersView parameters) throws Exception
    {
    }

    @Override
    public <T> T getValue(Class<T> clazz, RequiredAssetParametersView parameters) throws Exception
    {
        return null;
    }
}
