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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import com.netflix.governator.annotations.DefaultAssetLoader;
import com.netflix.governator.annotations.RequiredAsset;
import com.netflix.governator.annotations.RequiredAssets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.concurrent.GuardedBy;
import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
class AssetLoading
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ConcurrentMap<RequiredAsset, AssetLoaderMetadata> metadata = Maps.newConcurrentMap();
    private final Set<AssetLoader> assetLoaders;
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

    AssetLoading(Collection<AssetLoader> assetLoaders)
    {
        this.assetLoaders = ImmutableSet.copyOf(assetLoaders);

        AssetLoader     defaultAssetLoader = null;
        for ( AssetLoader loader : assetLoaders )
        {
            if ( loader.getClass().isAnnotationPresent(DefaultAssetLoader.class) )
            {
                if ( defaultAssetLoader != null )
                {
                    Preconditions.checkState(false, "More than one default asset loader: %s, %s", defaultAssetLoader.getClass().getName(), loader.getClass().getName());
                }
                defaultAssetLoader = loader;
            }
        }
        this.defaultAssetLoader = defaultAssetLoader;
    }

    public boolean     loadAssetsFor(Object obj) throws Exception
    {
        RequiredAsset requiredAsset = obj.getClass().getAnnotation(RequiredAsset.class);
        RequiredAssets requiredAssets = obj.getClass().getAnnotation(RequiredAssets.class);

        if ( requiredAsset != null )
        {
            internalLoadAsset(requiredAsset, obj, assetLoaders);
        }
        if ( requiredAssets != null )
        {
            for ( RequiredAsset asset : requiredAssets.value() )
            {
                internalLoadAsset(asset, obj, assetLoaders);
            }
        }

        return (requiredAsset != null) || (requiredAssets != null);
    }

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

                    loaderMetadata.loader.unloadAsset(requiredAsset.value());
                    loaderMetadata.isLoaded = false;
                }
            }
        }
    }

    private void internalLoadAsset(final RequiredAsset requiredAsset, Object obj, Collection<AssetLoader> assetLoaders) throws Exception
    {
        AssetLoader             loader;
        if ( requiredAsset.loader() == AssetLoader.class )
        {
            Preconditions.checkState(defaultAssetLoader != null, "No default asset loader was found");
            loader = defaultAssetLoader;
        }
        else
        {
            loader = Iterables.find
            (
                assetLoaders,
                new Predicate<AssetLoader>()
                {
                    @Override
                    public boolean apply(AssetLoader thisLoader)
                    {
                        return requiredAsset.loader().isAssignableFrom(thisLoader.getClass());
                    }
                },
                null
            );
            Preconditions.checkState(loader != null, "No asset loader found for: " + requiredAsset.loader().getName());
        }

        if ( loader == null )
        {
            log.error(String.format("No loader found for required asset named \"%s\" for class \"%s\"", requiredAsset.value(), obj.getClass().getName()));
            return;
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
