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

import com.google.common.collect.Maps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.annotation.concurrent.GuardedBy;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Arrays;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
@Singleton
public class AssetLoaderManager
{
    private final Logger            log = LoggerFactory.getLogger(getClass());
    private final ConcurrentMap<String, AssetHolder> assetStates = Maps.newConcurrentMap();
    private final ConcurrentMap<Class<? extends AssetLoader>, AssetLoaderHolder> loaders = Maps.newConcurrentMap();

    private static class AssetLoaderHolder
    {
        @GuardedBy("synchronization")
        AssetLoader        loader;
    }

    private static class AssetHolder
    {
        @GuardedBy("synchronization")
        boolean         isLoaded;

        final AtomicLong useCount = new AtomicLong(0);
    }

    @Inject
    public AssetLoaderManager()
    {
        // NOP
    }

    public void     addLoader(AssetLoader loader)
    {
        log.debug("Adding loader: " + loader.getClass().getName());

        AssetLoaderHolder   holder = getHolder(loader.getClass());
        synchronized(holder)
        {
            holder.loader = loader;
        }
    }

    public boolean     loadAssetsFor(Object obj) throws Exception
    {
        RequiredAsset   requiredAsset = obj.getClass().getAnnotation(RequiredAsset.class);
        if ( requiredAsset != null )
        {
            String          key = makeKey(requiredAsset.loader(), requiredAsset.name());

            AssetLoader     loader = getLoader(requiredAsset.loader());
            if ( loader != null )
            {
                AssetHolder     newAssetHolder = new AssetHolder();
                AssetHolder     oldAssetHolder = assetStates.putIfAbsent(key, newAssetHolder);
                AssetHolder     useAssetHolder = (oldAssetHolder != null) ? oldAssetHolder : newAssetHolder;
                synchronized(useAssetHolder)
                {
                    if ( !useAssetHolder.isLoaded )
                    {
                        log.debug(String.format("Loading assets using loader %s for asset \"%s\" with arguments: %s", requiredAsset.loader(), requiredAsset.name(), Arrays.toString(requiredAsset.arguments())));

                        loader.loadAsset(requiredAsset.name(), requiredAsset.arguments());
                        useAssetHolder.isLoaded = true;
                    }
                    useAssetHolder.useCount.incrementAndGet();
                }
            }
        }

        return (requiredAsset != null);
    }

    public void     unloadAssetsFor(Object obj) throws Exception
    {
        RequiredAsset   requiredAsset = obj.getClass().getAnnotation(RequiredAsset.class);
        if ( requiredAsset != null )
        {
            String          key = makeKey(requiredAsset.loader(), requiredAsset.name());

            AssetLoader     loader = getLoader(requiredAsset.loader());
            if ( loader != null )
            {
                AssetHolder assetHolder = assetStates.get(key);
                if ( assetHolder != null )
                {
                    synchronized(assetHolder)
                    {
                        long newCount = assetHolder.useCount.decrementAndGet();
                        if ( newCount <= 0 )
                        {
                            log.debug(String.format("Unloading assets using loader %s for asset \"%s\" with arguments: %s", requiredAsset.loader(), requiredAsset.name(), Arrays.toString(requiredAsset.arguments())));
                            if ( newCount < 0 )
                            {
                                log.error(String.format("Use count has gone negative (%d) for loader: %s with name: %s", newCount, requiredAsset.loader(), requiredAsset.name()));
                                assetHolder.useCount.set(0);
                            }

                            loader.unloadAsset(requiredAsset.name(), requiredAsset.arguments());
                            assetHolder.isLoaded = false;
                        }
                    }
                }
            }
        }
    }

    private AssetLoader getLoader(Class<? extends AssetLoader> loaderClass)
    {
        AssetLoaderHolder   holder = getHolder(loaderClass);
        synchronized(holder)
        {
            if ( holder.loader == null )
            {
                log.warn(String.format("No loader registered for %s. Allocating new loader.", loaderClass.getName()));
                try
                {
                    holder.loader = loaderClass.newInstance();
                }
                catch ( Throwable e )
                {
                    log.error(String.format("Could not allocate loader %s.", loaderClass.getName()), e);
                }
            }
            return holder.loader;
        }
    }

    private AssetLoaderHolder getHolder(Class<? extends AssetLoader> loaderClass)
    {
        AssetLoaderHolder   newHolder = new AssetLoaderHolder();
        AssetLoaderHolder   oldHolder = loaders.putIfAbsent(loaderClass, newHolder);
        return (oldHolder != null) ? oldHolder : newHolder;
    }

    private static String       makeKey(Class<? extends AssetLoader> loaderClass, String assetName)
    {
        return loaderClass.getName() + "\t" + assetName;
    }
}
