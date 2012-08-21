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

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.google.inject.Injector;
import com.netflix.governator.annotations.RequiredAsset;
import com.netflix.governator.annotations.RequiredAssets;
import com.netflix.governator.assets.AssetLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.concurrent.GuardedBy;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Used internally to manage asset loading
 */
@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
class AssetLoading
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ConcurrentMap<RequiredAsset, AssetLoaderMetadata> metadata = Maps.newConcurrentMap();
    private final Map<String, AssetLoader> assetLoaders;
    private final Injector injector;
    private final AssetLoader defaultAssetLoader;

    private static class AssetLoaderMetadata
    {
        @GuardedBy("synchronization")
        boolean             isLoaded;

        final AssetLoader   loader;
        final AtomicLong    useCount = new AtomicLong(0);

        private AssetLoaderMetadata(AssetLoader loader)
        {
            this.loader = loader;
        }
    }

    /**
     * @param injector the Guice injector or null
     * @param defaultAssetLoader the default asset loader or null
     * @param assetLoaders map of asset names to loaders
     */
    AssetLoading(Injector injector, AssetLoader defaultAssetLoader, Map<String, AssetLoader> assetLoaders)
    {
        this.injector = injector;
        this.defaultAssetLoader = defaultAssetLoader;
        this.assetLoaders = ImmutableMap.copyOf(assetLoaders);
    }

    /**
     * Load the assets (if any) for the given object
     *
     * @param obj object to load
     * @return true if the object has assets
     * @throws Exception errors
     */
    boolean     loadAssetsFor(Object obj) throws Exception
    {
        RequiredAsset requiredAsset = obj.getClass().getAnnotation(RequiredAsset.class);
        RequiredAssets requiredAssets = obj.getClass().getAnnotation(RequiredAssets.class);

        if ( requiredAsset != null )
        {
            internalLoadAsset(requiredAsset);
        }
        if ( requiredAssets != null )
        {
            for ( RequiredAsset asset : requiredAssets.value() )
            {
                internalLoadAsset(asset);
            }
        }

        return (requiredAsset != null) || (requiredAssets != null);
    }

    /**
     * Unload the assets (if any) for the given object
     *
     * @param obj object to unload
     * @throws Exception errors
     */
    void     unloadAssetsFor(Object obj) throws Exception
    {
        RequiredAsset requiredAsset = obj.getClass().getAnnotation(RequiredAsset.class);
        RequiredAssets requiredAssets = obj.getClass().getAnnotation(RequiredAssets.class);

        if ( requiredAsset != null )
        {
            internalUnloadAsset(requiredAsset);
        }
        if ( requiredAssets != null )
        {
            for ( RequiredAsset asset : requiredAssets.value() )
            {
                internalUnloadAsset(asset);
            }
        }
    }

    private void internalUnloadAsset(RequiredAsset requiredAsset) throws Exception
    {
        AssetLoaderMetadata     loaderMetadata = metadata.get(requiredAsset);
        if ( loaderMetadata != null )
        {
            synchronized(loaderMetadata)
            {
                long newCount = loaderMetadata.useCount.decrementAndGet();
                if ( newCount <= 0 )
                {
                    log.debug(String.format("Unloading required asset named \"%s\"", requiredAsset.value()));
                    if ( newCount < 0 )
                    {
                        log.debug(String.format("Use count has gone negative for required asset named \"%s\"", requiredAsset.value()));
                        loaderMetadata.useCount.set(0);
                    }

                    loaderMetadata.loader.unloadAsset(requiredAsset.value());
                    loaderMetadata.isLoaded = false;
                }
            }
        }
    }

    private void internalLoadAsset(RequiredAsset requiredAsset) throws Exception
    {
        AssetLoader             loader = assetLoaders.get(requiredAsset.value());
        if ( loader == null )
        {
            if ( (requiredAsset.loader() == AssetLoader.class) && (defaultAssetLoader != null) )
            {
                loader = defaultAssetLoader;
            }
            else
            {
                loader = injector.getInstance(requiredAsset.loader());
            }
        }

        AssetLoaderMetadata newAssetLoaderMetadata = new AssetLoaderMetadata(loader);
        AssetLoaderMetadata oldAssetLoaderMetadata = metadata.putIfAbsent(requiredAsset, newAssetLoaderMetadata);
        AssetLoaderMetadata useAssetLoaderMetadata = (oldAssetLoaderMetadata != null) ? oldAssetLoaderMetadata : newAssetLoaderMetadata;
        synchronized(useAssetLoaderMetadata)
        {
            if ( !useAssetLoaderMetadata.isLoaded )
            {
                log.debug(String.format("Loading required asset named \"%s\"", requiredAsset.value()));

                loader.loadAsset(requiredAsset.value());
                useAssetLoaderMetadata.isLoaded = true;
            }
            useAssetLoaderMetadata.useCount.incrementAndGet();
        }
    }
}
