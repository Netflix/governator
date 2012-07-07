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

package com.netflix.governator.assets;

import com.google.common.collect.ImmutableList;
import java.util.List;

public class MultiAssetLoader implements AssetLoader
{
    private final List<AssetLoader>         loaders;

    public MultiAssetLoader(List<AssetLoader> loaders)
    {
        this.loaders = ImmutableList.copyOf(loaders);
    }

    @Override
    public void loadAsset(String name, String[] args) throws Exception
    {
        for ( int i = 0; i < loaders.size(); ++i )
        {
            String[] thisArg = (i < args.length) ? new String[]{args[i]} : new String[]{};
            loaders.get(i).loadAsset(name, thisArg);
        }
    }

    @Override
    public void unloadAsset(String name, String[] args) throws Exception
    {
        for ( int i = (loaders.size() - 1); i >= 0; --i )   // reverse order
        {
            String[] thisArg = (i < args.length) ? new String[]{args[i]} : new String[]{};
            loaders.get(i).unloadAsset(name, thisArg);
        }
    }
}
