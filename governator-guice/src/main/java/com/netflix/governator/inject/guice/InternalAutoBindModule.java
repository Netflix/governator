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
import com.google.inject.AbstractModule;
import com.netflix.governator.lifecycle.ClasspathScanner;
import java.util.Collection;
import java.util.List;

class InternalAutoBindModule extends AbstractModule
{
    private final List<Class<?>> ignoreClasses;
    private final ClasspathScanner classpathScanner;

    InternalAutoBindModule(ClasspathScanner classpathScanner, Collection<Class<?>> ignoreClasses)
    {
        this.classpathScanner = classpathScanner;
        Preconditions.checkNotNull(ignoreClasses, "ignoreClasses cannot be null");

        this.ignoreClasses = ImmutableList.copyOf(ignoreClasses);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected void configure()
    {
        for ( final Class<?> clazz : classpathScanner.get() )
        {
            if ( ignoreClasses.contains(clazz) )
            {
                continue;
            }

            binder().bind(clazz).asEagerSingleton();

            if ( javax.inject.Provider.class.isAssignableFrom(clazz) )
            {
                ProviderBinderUtil.bind(binder(), (Class <? extends javax.inject.Provider>)clazz);
            }
        }
    }
}
