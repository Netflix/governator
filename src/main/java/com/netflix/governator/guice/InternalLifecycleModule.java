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
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.netflix.governator.lifecycle.LifecycleListener;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.LifecycleMethods;
import com.netflix.governator.lifecycle.warmup.DAGManager;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

class InternalLifecycleModule implements Module
{
    private final Map<Class<?>, LifecycleMethods> lifecycleMethods = Maps.newHashMap();
    private final AtomicReference<LifecycleManager> lifecycleManager;

    InternalLifecycleModule(AtomicReference<LifecycleManager> lifecycleManager)
    {
        this.lifecycleManager = lifecycleManager;
    }

    @Override
    public void configure(final Binder binder)
    {
        binder.bindListener
        (
            Matchers.any(),
            new TypeListener()
            {
                @Override
                public <T> void hear(final TypeLiteral<T> type, TypeEncounter<T> encounter)
                {
                    encounter.register
                    (
                        new InjectionListener<T>()
                        {
                            @Override
                            public void afterInjection(T obj)
                            {
                                LifecycleManager manager = lifecycleManager.get();
                                if ( manager != null )
                                {
                                    for ( LifecycleListener listener : manager.getListeners() )
                                    {
                                        listener.objectInjected(obj);
                                    }

                                    Class<?> clazz = obj.getClass();
                                    LifecycleMethods methods = getLifecycleMethods(clazz);

                                    addDependencies(manager, obj, type, methods);

                                    if ( methods.hasLifecycleAnnotations() )
                                    {
                                        try
                                        {
                                            manager.add(obj, methods);
                                        }
                                        catch ( Exception e )
                                        {
                                            throw new Error(e);
                                        }
                                    }
                                }
                            }
                        }
                    );
                }
            }
        );
    }

    private void addDependencies(LifecycleManager manager, Object obj, TypeLiteral<?> type, LifecycleMethods methods)
    {
        DAGManager              dagManager = manager.getDAGManager();
        dagManager.addObjectMapping(type, obj, methods);

        applyInjectionPoint(InjectionPoint.forConstructorOf(type), dagManager, type);
        for ( InjectionPoint injectionPoint : InjectionPoint.forInstanceMethodsAndFields(type) )
        {
            applyInjectionPoint(injectionPoint, dagManager, type);
        }
    }

    private void applyInjectionPoint(InjectionPoint injectionPoint, DAGManager dagManager, TypeLiteral<?> type)
    {
        if ( injectionPoint != null )
        {
            List<Dependency<?>> dependencies = injectionPoint.getDependencies();
            for ( Dependency<?> dependency : dependencies )
            {
                dagManager.addDependency(type, dependency.getKey().getTypeLiteral());
            }
        }
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