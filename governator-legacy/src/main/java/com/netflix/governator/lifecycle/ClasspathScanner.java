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

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import org.objectweb.asm.ClassReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;

import static org.objectweb.asm.ClassReader.*;

/**
 * Utility to find annotated classes
 */
public class ClasspathScanner
{
    private static final Logger log = LoggerFactory.getLogger(ClasspathScanner.class);
    protected final ClassLoader classLoader;
    
    private final Set<Class<?>> classes;
    private final Set<Constructor> constructors;
    private final Set<Method> methods;
    private final Set<Field> fields;

    /**
     * @param basePackages list of packages to search (recursively)
     * @param annotations class annotations to search for
     */
    public ClasspathScanner(Collection<String> basePackages, Collection<Class<? extends Annotation>> annotations)
    {
        this(basePackages, annotations, Thread.currentThread().getContextClassLoader());
    }

    /**
     * @param basePackages list of packages to search (recursively)
     * @param annotations class annotations to search for
     * @param classLoader ClassLoader containing the classes to be scanned
     */
    public ClasspathScanner(Collection<String> basePackages, Collection<Class<? extends Annotation>> annotations, final ClassLoader classLoader) 
    {
        Preconditions.checkNotNull(annotations, "annotations cannot be null");
        Preconditions.checkNotNull(classLoader, "classLoader cannot be null");

        log.debug("Starting classpath scanning...");
        this.classLoader = classLoader;

        Set<Class<?>>       localClasses = Sets.newHashSet();
        Set<Constructor>    localConstructors = Sets.newHashSet();
        Set<Method>         localMethods = Sets.newHashSet();
        Set<Field>          localFields = Sets.newHashSet();

        doScanning(basePackages, annotations, localClasses, localConstructors, localMethods, localFields);

        classes = ImmutableSet.copyOf(localClasses);
        constructors = ImmutableSet.copyOf(localConstructors);
        methods = ImmutableSet.copyOf(localMethods);
        fields = ImmutableSet.copyOf(localFields);

        log.debug("Classpath scanning done");
    }

    /**
     * @return the found classes
     */
    public Set<Class<?>> getClasses()
    {
        return classes;
    }

    public Set<Constructor> getConstructors()
    {
        return constructors;
    }

    public Set<Method> getMethods()
    {
        return methods;
    }

    public Set<Field> getFields()
    {
        return fields;
    }

    protected void doScanning(Collection<String> basePackages, Collection<Class<? extends Annotation>> annotations, Set<Class<?>> localClasses, Set<Constructor> localConstructors, Set<Method> localMethods, Set<Field> localFields)
    {
        if ( basePackages.isEmpty() )
        {
            log.warn("No base packages specified - no classpath scanning will be done");
            return;
        }
        try
        {
            for ( String basePackage : basePackages )
            {
                Enumeration<URL> resources = classLoader.getResources(basePackage.replace(".", "/"));
                while ( resources.hasMoreElements() )
                {
                    URL url = resources.nextElement();
                    if ( isJarURL(url))
                    {
                        String jarPath = url.getFile();
                        if ( jarPath.contains("!") )
                        {
                            jarPath = jarPath.substring(0, jarPath.indexOf("!"));
                            url = new URL(jarPath);
                        }

                        File file = ClasspathUrlDecoder.toFile(url);
                        try(JarFile jar = new JarFile(file))
                        {
                            for ( Enumeration<JarEntry> list = jar.entries(); list.hasMoreElements(); )
                            {
                                JarEntry entry = list.nextElement();
                                if ( entry.getName().endsWith(".class") )
                                {
                                    AnnotationFinder finder = new AnnotationFinder(classLoader, annotations.toArray(new Class[annotations.size()]));
                                    new ClassReader(jar.getInputStream(entry)).accept(finder, SKIP_CODE);

                                    localClasses.addAll(finder.getAnnotatedClasses());
                                    localMethods.addAll(finder.getAnnotatedMethods());
                                    localConstructors.addAll(finder.getAnnotatedConstructors());
                                    localFields.addAll(finder.getAnnotatedFields());
                                }
                            }
                        }
                        catch( IOException e )
                        {
                            throw new IllegalStateException("Governator was unable to scan " +
                                    file.getName() + " for annotations", e);
                        }
                    }
                    else
                    {
                        DirectoryClassFilter filter = new DirectoryClassFilter(classLoader);
                        for ( String className : filter.filesInPackage(url, basePackage) )
                        {
                            AnnotationFinder finder = new AnnotationFinder(classLoader, annotations.toArray(new Class[annotations.size()]));
                            new ClassReader(filter.bytecodeOf(className)).accept(finder, SKIP_CODE);

                            localClasses.addAll(finder.getAnnotatedClasses());
                            localMethods.addAll(finder.getAnnotatedMethods());
                            localConstructors.addAll(finder.getAnnotatedConstructors());
                            localFields.addAll(finder.getAnnotatedFields());
                        }
                    }
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
