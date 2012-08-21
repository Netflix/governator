package com.netflix.governator.guice.mocks;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import com.netflix.governator.assets.AssetLoader;
import org.testng.Assert;
import java.util.List;
import java.util.Map;

public class ParameterizedAssetLoader implements AssetLoader
{
    @Inject
    private Map<String, String> parameters = Maps.newHashMap();

    @Inject
    private Map<String, Map<String, Integer>> integerMap = Maps.newHashMap();

    @Inject(optional = true)
    private Map<String, List<String>> stringMap = Maps.newHashMap();

    @Inject
    public ParameterizedAssetLoader()
    {
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
