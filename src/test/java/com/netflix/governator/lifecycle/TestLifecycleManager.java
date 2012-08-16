/*
 * Copyright 2012 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.governator.lifecycle;

import com.google.common.collect.Maps;
import com.netflix.governator.assets.AssetLoader;
import com.netflix.governator.assets.AssetParameters;
import com.netflix.governator.assets.AssetParametersView;
import com.netflix.governator.assets.GenericParameterType;
import com.netflix.governator.configuration.SystemConfigurationProvider;
import com.netflix.governator.lifecycle.mocks.DuplicateAsset;
import com.netflix.governator.lifecycle.mocks.ParameterizedAssetLoader;
import com.netflix.governator.lifecycle.mocks.SimpleAssetLoader;
import com.netflix.governator.lifecycle.mocks.SimpleContainer;
import com.netflix.governator.lifecycle.mocks.SimpleHasAsset;
import com.netflix.governator.lifecycle.mocks.SimpleObject;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.HashMap;
import java.util.Map;

public class TestLifecycleManager
{
    @Test
    public void     testSimple() throws Exception
    {
        LifecycleManager    manager = new LifecycleManager();
        manager.start();

        SimpleObject        simpleObject = new SimpleObject();

        Assert.assertEquals(manager.getState(simpleObject), LifecycleState.LATENT);
        Assert.assertEquals(simpleObject.startCount.get(), 0);
        Assert.assertEquals(simpleObject.finishCount.get(), 0);

        manager.add(simpleObject);
        Assert.assertEquals(manager.getState(simpleObject), LifecycleState.ACTIVE);
        Assert.assertEquals(simpleObject.startCount.get(), 1);
        Assert.assertEquals(simpleObject.finishCount.get(), 0);

        manager.close();
        Assert.assertEquals(manager.getState(simpleObject), LifecycleState.LATENT);
        Assert.assertEquals(simpleObject.startCount.get(), 1);
        Assert.assertEquals(simpleObject.finishCount.get(), 1);
    }

    @Test
    public void     testSimpleContainer() throws Exception
    {
        LifecycleManager    manager = new LifecycleManager();
        manager.start();

        SimpleObject        simpleObject = new SimpleObject();
        manager.add(simpleObject);

        SimpleContainer     simpleContainer = new SimpleContainer(manager, simpleObject);
        manager.add(simpleContainer);

        Assert.assertEquals(manager.getState(simpleObject), LifecycleState.ACTIVE);
        Assert.assertEquals(simpleObject.startCount.get(), 1);
        Assert.assertEquals(simpleObject.finishCount.get(), 0);
        Assert.assertEquals(manager.getState(simpleContainer), LifecycleState.ACTIVE);
        Assert.assertEquals(simpleContainer.startCount.get(), 1);
        Assert.assertEquals(simpleContainer.finishCount.get(), 0);

        manager.close();

        Assert.assertEquals(manager.getState(simpleObject), LifecycleState.LATENT);
        Assert.assertEquals(simpleObject.startCount.get(), 1);
        Assert.assertEquals(simpleObject.finishCount.get(), 1);
        Assert.assertEquals(manager.getState(simpleContainer), LifecycleState.LATENT);
        Assert.assertEquals(simpleContainer.startCount.get(), 1);
        Assert.assertEquals(simpleContainer.finishCount.get(), 1);
    }

    @Test
    public void     testOverrideAssetLoader() throws Exception
    {
        SimpleAssetLoader               overrideAssetLoader = new SimpleAssetLoader();
        SimpleAssetLoader               simpleAssetLoader = new SimpleAssetLoader();
        HashMap<String, AssetLoader>    map = Maps.newHashMap();
        map.put(LifecycleManager.DEFAULT_ASSET_LOADER_VALUE, simpleAssetLoader);
        map.put("foo", overrideAssetLoader);
        LifecycleManager                manager = new LifecycleManager(map, Maps.<String, AssetParametersView>newHashMap(), new SystemConfigurationProvider());

        manager.add(new SimpleHasAsset());

        manager.start();

        Assert.assertEquals(simpleAssetLoader.loadedCount.get(), 0);
        Assert.assertEquals(simpleAssetLoader.unloadedCount.get(), 0);
        Assert.assertEquals(overrideAssetLoader.loadedCount.get(), 1);
        Assert.assertEquals(overrideAssetLoader.unloadedCount.get(), 0);

        manager.close();

        Assert.assertEquals(simpleAssetLoader.loadedCount.get(), 0);
        Assert.assertEquals(simpleAssetLoader.unloadedCount.get(), 0);
        Assert.assertEquals(overrideAssetLoader.loadedCount.get(), 1);
        Assert.assertEquals(overrideAssetLoader.unloadedCount.get(), 1);
    }

    @Test
    public void     testSimpleAsset() throws Exception
    {
        SimpleAssetLoader               simpleAssetLoader = new SimpleAssetLoader();
        HashMap<String, AssetLoader>    map = Maps.newHashMap();
        map.put(LifecycleManager.DEFAULT_ASSET_LOADER_VALUE, simpleAssetLoader);
        LifecycleManager    manager = new LifecycleManager(map, Maps.<String, AssetParametersView>newHashMap(), new SystemConfigurationProvider());
        manager.start();

        SimpleHasAsset      simpleHasAsset = new SimpleHasAsset();
        manager.add(simpleHasAsset);

        Assert.assertEquals(manager.getState(simpleHasAsset), LifecycleState.ACTIVE);
        Assert.assertEquals(simpleHasAsset.startCount.get(), 1);
        Assert.assertEquals(simpleHasAsset.finishCount.get(), 0);

        Assert.assertEquals(simpleAssetLoader.loadedCount.get(), 1);
        Assert.assertEquals(simpleAssetLoader.unloadedCount.get(), 0);

        manager.close();

        Assert.assertEquals(manager.getState(simpleHasAsset), LifecycleState.LATENT);
        Assert.assertEquals(simpleHasAsset.startCount.get(), 1);
        Assert.assertEquals(simpleHasAsset.finishCount.get(), 1);

        Assert.assertEquals(simpleAssetLoader.loadedCount.get(), 1);
        Assert.assertEquals(simpleAssetLoader.unloadedCount.get(), 1);
    }

    @Test
    public void     testDuplicateAsset() throws Exception
    {
        SimpleAssetLoader               simpleAssetLoader = new SimpleAssetLoader();
        HashMap<String, AssetLoader>    map = Maps.newHashMap();
        map.put(LifecycleManager.DEFAULT_ASSET_LOADER_VALUE, simpleAssetLoader);
        LifecycleManager    manager = new LifecycleManager(map, Maps.<String, AssetParametersView>newHashMap(), new SystemConfigurationProvider());
        manager.add(new SimpleHasAsset(), new DuplicateAsset());
        manager.start();

        Assert.assertEquals(simpleAssetLoader.loadedCount.get(), 1);
        Assert.assertEquals(simpleAssetLoader.unloadedCount.get(), 0);

        manager.close();

        Assert.assertEquals(simpleAssetLoader.loadedCount.get(), 1);
        Assert.assertEquals(simpleAssetLoader.unloadedCount.get(), 1);
    }

    @Test
    public void     testAssetParameters() throws Exception
    {
        ParameterizedAssetLoader        loader = new ParameterizedAssetLoader();
        HashMap<String, AssetLoader>    map = Maps.newHashMap();
        map.put("foo", loader);

        AssetParameters                 assetParameters = new AssetParameters();
        Map<String, String>             parameter = Maps.newHashMap();
        parameter.put("one", "1");
        parameter.put("two", "2");
        parameter.put("three", "3");
        assetParameters.set(new GenericParameterType<Map<String, String>>(){}, parameter);

        Map<String, AssetParametersView>    parametersMaps = Maps.newHashMap();
        parametersMaps.put("foo", assetParameters);
        LifecycleManager                manager = new LifecycleManager(map, parametersMaps, new SystemConfigurationProvider());
        manager.add(new SimpleHasAsset());
        // assertions are in AssetParameters
    }
}
