package com.netflix.governator.guice.mocks;

import com.google.inject.Inject;
import com.netflix.governator.assets.AssetLoader;
import org.testng.Assert;
import java.util.Map;

public class ParameterizedAssetLoader implements AssetLoader
{
    private final Map<String, String> parameters;
    private final Map<String, Map<String, Integer>> integerMap;

    @Inject
    public ParameterizedAssetLoader(Map<String, String> parameters, Map<String, Map<String, Integer>> integerMap)
    {
        this.parameters = parameters;
        this.integerMap = integerMap;
    }

    @Override
    public void loadAsset(String name) throws Exception
    {
        Assert.assertEquals(parameters.get("test"), "Yes it's a test");

        Assert.assertEquals(integerMap.get("test").get("one").intValue(), 1);
        Assert.assertEquals(integerMap.get("test").get("two").intValue(), 2);
        Assert.assertEquals(integerMap.get("test").get("three").intValue(), 3);
    }

    @Override
    public void unloadAsset(String name) throws Exception
    {
    }
}
