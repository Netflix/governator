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
import java.util.Map;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

@SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
@Singleton
public class AssetLoaderManager
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final ConcurrentMap<AssetLoaderBinding, AssetLoader> loaders;
    private final ConcurrentMap<AssetLoaderBinding, AssetLoaderMetadata> metadata = Maps.newConcurrentMap();

    private static class AssetLoaderMetadata
    {
        @GuardedBy("synchronization")
        boolean             isLoaded;

        final AtomicLong    useCount = new AtomicLong(0);
    }

    private static class BindingAndLoader
    {
        final AssetLoaderBinding binding;
        final AssetLoader                               loader;

        private BindingAndLoader(AssetLoaderBinding binding, AssetLoader loader)
        {
            this.binding = binding;
            this.loader = loader;
        }
    }

    public AssetLoaderManager()
    {
        this.loaders = Maps.newConcurrentMap();
    }

    @Inject
    public AssetLoaderManager(Map<AssetLoaderBinding, AssetLoader> loaders)
    {
        this();
        this.loaders.putAll(loaders);
    }

    public AssetLoader  getLoader(AssetLoaderBinding binding) throws Exception
    {
        BindingAndLoader    bindingAndLoader = getBindingAndLoader(binding);
        return bindingAndLoader.loader;
    }

    public boolean     loadAssetsFor(Object obj) throws Exception
    {
        RequiredAsset   requiredAsset = obj.getClass().getAnnotation(RequiredAsset.class);
        RequiredAssets  requiredAssets = obj.getClass().getAnnotation(RequiredAssets.class);

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

    public void     unloadAssetsFor(Object obj) throws Exception
    {
        RequiredAsset   requiredAsset = obj.getClass().getAnnotation(RequiredAsset.class);
        if ( requiredAsset != null )
        {
            BindingAndLoader                    bindingAndLoader = getBindingAndLoader(requiredAsset);
            if ( bindingAndLoader != null )
            {
                AssetLoaderMetadata loaderMetadata = metadata.get(bindingAndLoader.binding);
                if ( loaderMetadata != null )
                {
                    synchronized(loaderMetadata)
                    {
                        long newCount = loaderMetadata.useCount.decrementAndGet();
                        if ( newCount <= 0 )
                        {
                            log.debug(String.format("Unloading required asset named \"%s\" using loader \"%s\"", requiredAsset.name(), requiredAsset.loader().getName()));
                            if ( newCount < 0 )
                            {
                                log.debug(String.format("Use count has gone negative for required asset named \"%s\" using loader \"%s\"", requiredAsset.name(), requiredAsset.loader().getName()));
                                loaderMetadata.useCount.set(0);
                            }

                            bindingAndLoader.loader.unloadAsset(requiredAsset.name());
                            loaderMetadata.isLoaded = false;
                        }
                    }
                }
            }
        }
    }

    private void internalLoadAsset(RequiredAsset requiredAsset) throws Exception
    {
        BindingAndLoader        bindingAndLoader = getBindingAndLoader(requiredAsset);
        if ( bindingAndLoader == null )
        {
            log.error(String.format("No loader found for required asset named \"%s\" using loader \"%s\"", requiredAsset.name(), requiredAsset.loader().getName()));
            return;
        }

        AssetLoaderMetadata newAssetLoaderMetadata = new AssetLoaderMetadata();
        AssetLoaderMetadata oldAssetLoaderMetadata = metadata.putIfAbsent(bindingAndLoader.binding, newAssetLoaderMetadata);
        AssetLoaderMetadata useAssetLoaderMetadata = (oldAssetLoaderMetadata != null) ? oldAssetLoaderMetadata : newAssetLoaderMetadata;
        synchronized(useAssetLoaderMetadata)
        {
            if ( !useAssetLoaderMetadata.isLoaded )
            {
                log.debug(String.format("Loading required asset named \"%s\" using loader \"%s\"", requiredAsset.name(), requiredAsset.loader().getName()));

                bindingAndLoader.loader.loadAsset(requiredAsset.name());
                useAssetLoaderMetadata.isLoaded = true;
            }
            useAssetLoaderMetadata.useCount.incrementAndGet();
        }
    }

    private BindingAndLoader getBindingAndLoader(RequiredAsset requiredAsset) throws IllegalAccessException, InstantiationException
    {
        return getBindingAndLoader(new AssetLoaderBinding(requiredAsset.loader(), requiredAsset.name()));
    }

    private BindingAndLoader getBindingAndLoader(AssetLoaderBinding binding) throws IllegalAccessException, InstantiationException
    {
        AssetLoader            loader = loaders.get(binding);
        if ( loader == null )
        {
            AssetLoaderBinding     defaultBinding = new AssetLoaderBinding(binding.getLoaderClass(), AssetLoaderBinding.DEFAULT_BINDING_NAME);
            AssetLoader            defaultLoader = loaders.get(binding);
            if ( defaultLoader == null )
            {
                log.debug(String.format("No loader specified for name \"%s\" loader-class \"%s\". Creating a default.", binding.getName(), binding.getLoaderClass().getName()));

                AssetLoader newLoader = binding.getLoaderClass().newInstance();
                AssetLoader oldLoader = loaders.putIfAbsent(defaultBinding, newLoader);
                defaultLoader = (oldLoader != null) ? oldLoader : newLoader;
            }
            return new BindingAndLoader(defaultBinding, defaultLoader);
        }
        return new BindingAndLoader(binding, loader);
    }
}
