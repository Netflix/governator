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
import com.netflix.governator.inject.ClasspathScanner;
import java.util.Collection;

public class GuiceAutoBindModule extends AbstractModule
{
    private final Collection<Class<?>> ignoreClasses;

    public GuiceAutoBindModule()
    {
        this(Lists.<Class<?>>newArrayList());
    }

    public GuiceAutoBindModule(Collection<Class<?>> ignoreClasses)
    {
        Preconditions.checkNotNull(ignoreClasses, "ignoreClasses cannot be null");

        this.ignoreClasses = ImmutableList.copyOf(ignoreClasses);
    }

    @Override
    protected void configure()
    {
        ClasspathScanner        scanner = new ClasspathScanner(ClasspathScanner.getDefaultAnnotations(), ignoreClasses);
        for ( Class<?> clazz : scanner.get() )
        {
            binder().bind(clazz).asEagerSingleton();
        }
    }
}
