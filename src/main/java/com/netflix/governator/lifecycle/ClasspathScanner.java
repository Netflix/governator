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

package com.netflix.governator.lifecycle;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.xbean.finder.AnnotationFinder;
import org.apache.xbean.finder.archive.Archive;
import org.apache.xbean.finder.archive.CompositeArchive;
import org.apache.xbean.finder.archive.JarArchive;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

/**
 * Utility to find annotated classes
 */
public class ClasspathScanner
{
    private static final Logger log = LoggerFactory.getLogger(ClasspathScanner.class);
    private final Set<Class<?>> classes;

    /**
     * @param basePackages list of packages to search (recursively)
     * @param annotations class annotations to search for
     */
    public ClasspathScanner(Collection<String> basePackages, Collection<Class<? extends Annotation>> annotations)
    {
        Preconditions.checkNotNull(annotations, "additionalAnnotations cannot be null");

        log.debug("Starting classpath scanning...");

        final Set<Class<?>>     store = Sets.newHashSet();
        if ( basePackages.size() == 0 )
        {
            log.warn("No base packages specified - no classpath scanning will be done");
        }
        else
        {
            doScanning(basePackages, annotations, store);
        }

        classes = ImmutableSet.copyOf(store);
        log.debug("Classpath scanning done");
    }

    /**
     * @return the found classes
     */
    public Set<Class<?>> get()
    {
        return classes;
    }

    private void doScanning(Collection<String> basePackages, Collection<Class<? extends Annotation>> annotations, Set<Class<?>> store)
    {
        try
        {
            List<Archive> archives = Lists.newArrayList();
            ClassLoader         contextClassLoader = Thread.currentThread().getContextClassLoader();
            for ( String basePackage : basePackages )
            {
                Enumeration<URL> resources = contextClassLoader.getResources(basePackage.replace(".", "/"));
                while ( resources.hasMoreElements() )
                {
                    URL thisUrl = resources.nextElement();
                    if ( isJarURL(thisUrl))
                    {
                        archives.add(new JarArchive(contextClassLoader, thisUrl));
                    }
                    else
                    {
                        archives.add(new GovernatorFileArchive(contextClassLoader, thisUrl, basePackage));
                    }
                }
                CompositeArchive compositeArchive = new CompositeArchive(archives);
                AnnotationFinder annotationFinder = new AnnotationFinder(compositeArchive);
                for ( Class<? extends Annotation> annotation : annotations )
                {
                    store.addAll(annotationFinder.findAnnotatedClasses(annotation));
                }
            }
        }
        catch ( IOException e )
        {
            throw new RuntimeException(e);
        }
    }

    private boolean isJarURL(URL url)
    {
        String protocol = url.getProtocol();
        return "zip".equals(protocol) || "jar".equals(protocol);
    }
}
