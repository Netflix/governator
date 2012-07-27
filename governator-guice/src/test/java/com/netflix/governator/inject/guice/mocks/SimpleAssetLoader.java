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

package com.netflix.governator.inject.guice.mocks;

import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.annotations.DefaultAssetLoader;
import com.netflix.governator.lifecycle.AssetLoader;
import java.util.concurrent.atomic.AtomicInteger;

@AutoBindSingleton
@DefaultAssetLoader
public class SimpleAssetLoader implements AssetLoader
{
    public static final AtomicInteger      loadAssetCount = new AtomicInteger(0);
    public static final AtomicInteger      unloadAssetCount = new AtomicInteger(0);

    @Override
    public void loadAsset(String name) throws Exception
    {
        loadAssetCount.incrementAndGet();
    }

    @Override
    public void unloadAsset(String name) throws Exception
    {
        unloadAssetCount.incrementAndGet();
    }

    @Override
    public <T> T getValue(Class<T> clazz) throws Exception
    {
        return null;
    }
}
