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

/*
 * Based on work from the Proofpoint Platform published using the same Apache License, Version 2.0
 * https://github.com/proofpoint/platform
 */

package com.netflix.governator.guice;

import com.google.common.collect.Maps;
import com.google.inject.Binder;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.netflix.governator.guice.lazy.LazySingletonScope;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.LifecycleMethods;
import java.util.Map;

class InternalLifecycleModule implements Module
{
    private final Map<Class<?>, LifecycleMethods> lifecycleMethods = Maps.newHashMap();
    private final LifecycleManager lifecycleManager;

    InternalLifecycleModule(LifecycleManager lifecycleManager)
    {
        this.lifecycleManager = lifecycleManager;
    }

    @Override
    public void configure(final Binder binder)
    {
        binder.bindScope(LazySingleton.class, LazySingletonScope.get());

        binder.bindListener
        (
            Matchers.any(),
            new TypeListener()
            {
                @Override
                public <T> void hear(TypeLiteral<T> type, TypeEncounter<T> encounter)
                {
                    encounter.register
                    (
                        new InjectionListener<T>()
                        {
                            @Override
                            public void afterInjection(T obj)
                            {
                                if ( lifecycleManager.getListener() != null )
                                {
                                    lifecycleManager.getListener().objectInjected(obj);
                                }

                                Class<?> clazz = obj.getClass();
                                LifecycleMethods methods = getLifecycleMethods(clazz);

                                if ( methods.hasLifecycleAnnotations() )
                                {
                                    try
                                    {
                                        lifecycleManager.add(obj, methods);
                                    }
                                    catch ( Exception e )
                                    {
                                        throw new Error(e);
                                    }
                                }
                            }
                        }
                    );
                }
            }
        );
    }

    @Provides
    @Singleton
    public LifecycleManager     getLifecycleManager()
    {
        return lifecycleManager;
    }

    private LifecycleMethods getLifecycleMethods(Class<?> clazz)
    {
        LifecycleMethods methods = lifecycleMethods.get(clazz);
        if ( methods == null )
        {
            methods = new LifecycleMethods(clazz);
            lifecycleMethods.put(clazz, methods);
        }
        return methods;
    }
}