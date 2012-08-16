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

package com.netflix.governator.annotations;

import com.netflix.governator.assets.AssetLoader;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a class as a requiring an asset. Governator will load the asset
 * via the bound asset loader as part of post construction. The annotation
 * value will be passed to the loader. The default loader is used unless an override
 * loader is specified by {@link #loader()}. The default loader is an instance
 * of {@link AssetLoader} annotated with {@link AutoBindSingleton}.
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(java.lang.annotation.ElementType.TYPE)
public @interface RequiredAsset
{
    /**
     * @return the name of the asset to load
     */
    String      value();

    /**
     * @return The loader to use or <code>AssetLoader.class</code> for the default loader
     */
    Class<? extends AssetLoader>    loader() default AssetLoader.class;
}
