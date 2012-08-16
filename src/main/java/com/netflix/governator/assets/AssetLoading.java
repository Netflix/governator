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

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import com.netflix.governator.annotations.RequiredAsset;
import com.netflix.governator.annotations.RequiredAssets;
import com.netflix.governator.lifecycle.LifecycleManager;
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
public class AssetLoading
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ConcurrentMap<RequiredAsset, AssetLoaderMetadata> metadata = Maps.newConcurrentMap();
    private final Map<String, AssetLoader> assetLoaders;
    private final Map<String, AssetParametersView> parameters;

    private static final AssetParameters        nullParameters = new AssetParameters();

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
     * @param assetLoaders map of asset names to loaders
     * @param parameters map of asset names to parameters
     */
    public AssetLoading(Map<String, AssetLoader> assetLoaders, Map<String, AssetParametersView> parameters)
    {
        this.parameters = ImmutableMap.copyOf(parameters);
        this.assetLoaders = ImmutableMap.copyOf(assetLoaders);
    }

    /**
     * @return return the asset loader map
     */
    public Map<String, AssetLoader> getAssetLoaders()
    {
        return assetLoaders;
    }

    /**
     * @return return the parameters map
     */
    public Map<String, AssetParametersView> getParameters()
    {
        return parameters;
    }

    /**
     * Load the assets (if any) for the given object
     *
     * @param obj object to load
     * @return true if the object has assets
     * @throws Exception errors
     */
    public boolean     loadAssetsFor(Object obj) throws Exception
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
    public void     unloadAssetsFor(Object obj) throws Exception
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

                    loaderMetadata.loader.unloadAsset(requiredAsset.value(), getParameters(requiredAsset.value()));
                    loaderMetadata.isLoaded = false;
                }
            }
        }
    }

    private void internalLoadAsset(final RequiredAsset requiredAsset) throws Exception
    {
        AssetLoader             loader = assetLoaders.get(requiredAsset.value());
        if ( loader == null )
        {
            loader = assetLoaders.get(LifecycleManager.DEFAULT_ASSET_LOADER_VALUE);
            loader = Preconditions.checkNotNull(loader, "No mapped loader found and no default loader for: " + requiredAsset.value());
        }

        AssetLoaderMetadata newAssetLoaderMetadata = new AssetLoaderMetadata(loader);
        AssetLoaderMetadata oldAssetLoaderMetadata = metadata.putIfAbsent(requiredAsset, newAssetLoaderMetadata);
        AssetLoaderMetadata useAssetLoaderMetadata = (oldAssetLoaderMetadata != null) ? oldAssetLoaderMetadata : newAssetLoaderMetadata;
        synchronized(useAssetLoaderMetadata)
        {
            if ( !useAssetLoaderMetadata.isLoaded )
            {
                log.debug(String.format("Loading required asset named \"%s\"", requiredAsset.value()));

                loader.loadAsset(requiredAsset.value(), getParameters(requiredAsset.value()));
                useAssetLoaderMetadata.isLoaded = true;
            }
            useAssetLoaderMetadata.useCount.incrementAndGet();
        }
    }

    private AssetParametersView getParameters(String value)
    {
        AssetParametersView assetParameters = parameters.get(value);
        return (assetParameters != null) ? assetParameters : nullParameters;
    }
}
