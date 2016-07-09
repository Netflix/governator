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

package com.netflix.governator.guice;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.ProvisionListener;
import com.netflix.governator.lifecycle.LifecycleListener;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.LifecycleMethods;

class InternalLifecycleModule extends AbstractModule implements ProvisionListener {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalLifecycleModule.class);
    private final LoadingCache<Class<?>, LifecycleMethods> lifecycleMethods = CacheBuilder
        .newBuilder()
        .softValues()
        .build(new CacheLoader<Class<?>, LifecycleMethods>(){
            @Override
            public LifecycleMethods load(Class<?> key) throws Exception {
                return new LifecycleMethods(key);
            }});
    
    private final AtomicReference<LifecycleManager> lifecycleManager;

    InternalLifecycleModule(AtomicReference<LifecycleManager> lifecycleManager) {
        this.lifecycleManager = lifecycleManager;
    }

    @Override
    public void configure() {
        bindListener(
                Matchers.any(),
               this
            ); 
    }
    
    @Override
    public <T> void onProvision(ProvisionInvocation<T> provision) {
        T instance = provision.provision();
        if (instance != null) {
            LOGGER.trace("provisioning instance of {}", provision.getBinding().getKey());
            processInjectedObject(instance, provision.getBinding());
        }
    }

    private <T> void processInjectedObject(T obj, Binding<T> binding){
        LifecycleManager manager = lifecycleManager.get();
        if ( manager != null ) {
            for ( LifecycleListener listener : manager.getListeners() ) {
                listener.objectInjected(binding.getKey().getTypeLiteral(), obj);
            }

            Class<?> clazz = obj.getClass();
            LifecycleMethods methods = getLifecycleMethods(clazz);

            if ( methods.hasLifecycleAnnotations() ) {
                try {
                    manager.add(obj, binding, methods);
                }
                catch ( Throwable e ) {
                    throw new Error(e);
                }
            }
        }
    }

    private LifecycleMethods getLifecycleMethods(Class<?> clazz) {
        try {
            return lifecycleMethods.get(clazz);
        }
        catch ( ExecutionException e ) {
            throw new RuntimeException(e);
        }
    }
    

}
