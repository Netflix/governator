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

import com.netflix.governator.annotations.RequiredAsset;

/**
 * Abstraction for loading classes annotated with {@link RequiredAsset}
 */
public interface AssetLoader
{
    /**
     * Called to load the named asset
     *
     * @param name name of the asset
     * @throws Exception errors
     */
    public void     loadAsset(String name) throws Exception;

    /**
     * Called to unload the named asset
     *
     * @param name name of the asset
     * @throws Exception errors
     */
    public void     unloadAsset(String name) throws Exception;
}
