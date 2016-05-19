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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.netflix.governator.lifecycle.LifecycleListener;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.LifecycleMethods;

class InternalLifecycleModule extends AbstractModule {
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
            new TypeListener(){
                @Override
                public <T> void hear(final TypeLiteral<T> type, TypeEncounter<T> encounter) {
                    encounter.register(
                        new InjectionListener<T>() {
                            @Override
                            public void afterInjection(T obj) {
                                processInjectedObject(obj, type);
                            }
                        }
                    );
                }
            }
        );
    }

    private <T> void processInjectedObject(T obj, TypeLiteral<T> type){
        LifecycleManager manager = lifecycleManager.get();
        if ( manager != null ) {
            for ( LifecycleListener listener : manager.getListeners() ) {
                listener.objectInjected(type, obj);
            }

            Class<?> clazz = obj.getClass();
            LifecycleMethods methods = getLifecycleMethods(clazz);

            if ( methods.hasLifecycleAnnotations() ) {
                try {
                    manager.add(obj, methods);
                }
                catch ( Exception e ) {
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