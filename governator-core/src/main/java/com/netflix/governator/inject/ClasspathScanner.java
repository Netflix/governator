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

import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.sun.jersey.server.impl.container.config.AnnotatedClassScanner;
import java.io.File;
import java.lang.annotation.Annotation;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public class ClasspathScanner
{
    private final Set<Class<?>> classes;

    public static List<Class<? extends Annotation>> getDefaultAnnotations()
    {
        List<Class<? extends Annotation>>   annotations = Lists.newArrayList();
        annotations.add(AutoBindSingleton.class);
        return annotations;
    }

    public ClasspathScanner()
    {
        this(getDefaultAnnotations(), Lists.<Class<?>>newArrayList());
    }

    public ClasspathScanner(Collection<Class<? extends Annotation>> annotations)
    {
        this(annotations, Lists.<Class<?>>newArrayList());
    }

    public ClasspathScanner(Collection<Class<? extends Annotation>> annotations, final Collection<Class<?>> ignoreClasses)
    {
        Preconditions.checkNotNull(annotations, "additionalAnnotations cannot be null");
        Preconditions.checkNotNull(ignoreClasses, "ignoreClasses cannot be null");

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
                    return !ignoreClasses.contains(clazz);
                }
            }
        );
        classes = ImmutableSet.copyOf(filtered);
    }

    public Set<Class<?>> get()
    {
        return classes;
    }
}
