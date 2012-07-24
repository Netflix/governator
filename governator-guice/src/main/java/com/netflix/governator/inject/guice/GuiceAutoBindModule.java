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

package com.netflix.governator.inject.guice;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.netflix.governator.lifecycle.ClasspathScanner;
import java.util.Collection;

public class GuiceAutoBindModule extends AbstractModule
{
    private final Collection<Class<?>> ignoreClasses;
    private final LifecycleManagerInjector lifecycleManagerInjector;

    public GuiceAutoBindModule()
    {
        this(null, Lists.<Class<?>>newArrayList());
    }

    public GuiceAutoBindModule(LifecycleManagerInjector lifecycleManagerInjector)
    {
        this(lifecycleManagerInjector, Lists.<Class<?>>newArrayList());
    }

    public GuiceAutoBindModule(Collection<Class<?>> ignoreClasses)
    {
        this(null, ignoreClasses);
    }

    public GuiceAutoBindModule(LifecycleManagerInjector lifecycleManagerInjector, Collection<Class<?>> ignoreClasses)
    {
        this.lifecycleManagerInjector = lifecycleManagerInjector;
        Preconditions.checkNotNull(ignoreClasses, "ignoreClasses cannot be null");

        this.ignoreClasses = ImmutableList.copyOf(ignoreClasses);
    }

    public static void      bindProvider(Binder binder, Class<? extends javax.inject.Provider> clazz)
    {
        internalBindProvider(binder, clazz);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void configure()
    {
        ClasspathScanner        scanner = (lifecycleManagerInjector != null) ? lifecycleManagerInjector.getScanner() : new ClasspathScanner(ClasspathScanner.getDefaultAnnotations(), ignoreClasses);
        for ( final Class<?> clazz : scanner.get() )
        {
            if ( ignoreClasses.contains(clazz) )
            {
                continue;
            }

            binder().bind(clazz).asEagerSingleton();

            if ( javax.inject.Provider.class.isAssignableFrom(clazz) )
            {
                internalBindProvider(binder(), clazz);
            }
        }
    }

    private static void internalBindProvider(Binder binder, final Class<?> clazz)
    {
        Class<?> providedType;
        try
        {
            providedType = clazz.getMethod("get").getReturnType();
        }
        catch ( NoSuchMethodException e )
        {
            throw new RuntimeException(e);
        }

        binder
            .bind(providedType)
            .toProvider
                (
                    new Provider()
                    {
                        @Inject
                        private Injector       injector;

                        @Override
                        public Object get()
                        {
                            javax.inject.Provider provider = (javax.inject.Provider)injector.getInstance(clazz);   // cast is safe due to isAssignableFrom() check above
                            return provider.get();
                        }
                    }
                );
    }
}
