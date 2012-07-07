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

package com.netflix.governator.inject;

import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.netflix.governator.assets.AssetLoaderManager;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.sun.jersey.server.impl.container.config.AnnotatedClassScanner;
import javax.inject.Singleton;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class ClasspathScanner
{
    private final Set<Class<?>> classes;

    public ClasspathScanner(Class... additionalAnnotations)
    {
        /*
            Based on com.sun.jersey.api.core.ClasspathResourceConfig
         */
        String classPath = System.getProperty("java.class.path");
        String[] paths = classPath.split(File.pathSeparator);

        File[] roots = new File[paths.length];
        for ( int i = 0; i < paths.length; i++ )
        {
            roots[i] = new File(paths[i]);
        }

        if ( additionalAnnotations == null )
        {
            additionalAnnotations = new Class[0];
        }

        List<Class>           annotations = Lists.newArrayList(Arrays.asList((Class)Singleton.class, (Class)AutoBindSingleton.class));
        annotations.addAll(Arrays.asList(additionalAnnotations));
        AnnotatedClassScanner scanner = new AnnotatedClassScanner(annotations.toArray(new Class[annotations.size()]));
        scanner.scan(roots);

        Iterable<Class<?>> filtered = Iterables.filter
        (
            scanner.getMatchingClasses(),
            new Predicate<Class<?>>()
            {
                @Override
                public boolean apply(Class<?> clazz)
                {
                    return (clazz != LifecycleManager.class) && (clazz != AssetLoaderManager.class);
                }
            }
        );
        classes = ImmutableSet.copyOf(filtered);
    }

    public Set<Class<?>> getAll()
    {
        return classes;
    }

    public Set<Class<?>> getAutoBindSingletons()
    {
        Iterable<Class<?>> filtered = Iterables.filter
            (
                classes,
                new Predicate<Class<?>>()
                {
                    @Override
                    public boolean apply(Class<?> clazz)
                    {
                        return clazz.isAnnotationPresent(AutoBindSingleton.class);
                    }
                }
            );
        return ImmutableSet.copyOf(filtered);
    }
}
