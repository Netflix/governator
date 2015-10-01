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

import static org.objectweb.asm.ClassReader.SKIP_CODE;

import java.io.File;
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

/**
 * Utility to find annotated classes
 */
public class ClasspathScanner {
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
    public ClasspathScanner(Collection<String> basePackages, Collection<Class<? extends Annotation>> annotations) {
        this(basePackages, annotations, Thread.currentThread().getContextClassLoader());
    }

    /**
     * @param basePackages list of packages to search (recursively)
     * @param annotations class annotations to search for
     * @param classLoader ClassLoader containing the classes to be scanned
     */
    public ClasspathScanner(Collection<String> basePackages, Collection<Class<? extends Annotation>> annotations, final ClassLoader classLoader)  {
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
    public Set<Class<?>> getClasses() {
        return classes;
    }

    public Set<Constructor> getConstructors() {
        return constructors;
    }

    public Set<Method> getMethods() {
        return methods;
    }

    public Set<Field> getFields() {
        return fields;
    }

    protected void doScanning(Collection<String> basePackages, Collection<Class<? extends Annotation>> annotations, Set<Class<?>> localClasses, Set<Constructor> localConstructors, Set<Method> localMethods, Set<Field> localFields) {
        if ( basePackages.isEmpty() ) {
            log.warn("No base packages specified - no classpath scanning will be done");
            return;
        }
        log.info("Scanning packages : " + basePackages + " for annotations " + annotations);
        
        for ( String basePackage : basePackages )  {
            try {
            	String basePackageWithSlashes = basePackage.replace(".", "/");
            	Enumeration<URL> resources = classLoader.getResources(basePackageWithSlashes);
                while ( resources.hasMoreElements() ) {
                    URL url = resources.nextElement();
                    try {
                        if ( isJarURL(url)) {
                            String jarPath = url.getFile();
                            if ( jarPath.contains("!") ) {
                                jarPath = jarPath.substring(0, jarPath.indexOf("!"));
                                url = new URL(jarPath);
                            }
                            File file = ClasspathUrlDecoder.toFile(url);
                            try (JarFile jar = new JarFile(file)) {
                                for ( Enumeration<JarEntry> list = jar.entries(); list.hasMoreElements(); ) {
                                    JarEntry entry = list.nextElement();
                                    try {
                                        if ( entry.getName().endsWith(".class") && entry.getName().startsWith(basePackageWithSlashes)) {
                                            AnnotationFinder finder = new AnnotationFinder(classLoader, annotations);
                                            new ClassReader(jar.getInputStream(entry)).accept(finder, SKIP_CODE);
        
                                            applyFinderResults(localClasses, localConstructors, localMethods, localFields, finder);
                                        }
                                    }
                                    catch (Exception e) {
                                        log.debug("Unable to scan JarEntry '{}' in '{}'. {}", new Object[]{entry.getName(), file.getCanonicalPath(), e.getMessage()});
                                    }
                                }
                            }
                            catch (Exception e ) {
                                log.debug("Unable to scan '{}'. {}", new Object[]{file.getCanonicalPath(), e.getMessage()});
                            }
                        }
                        else {
                            DirectoryClassFilter filter = new DirectoryClassFilter(classLoader);
                            for ( String className : filter.filesInPackage(url, basePackage) ) {
                                AnnotationFinder finder = new AnnotationFinder(classLoader, annotations);
                                new ClassReader(filter.bytecodeOf(className)).accept(finder, SKIP_CODE);
    
                                applyFinderResults(localClasses, localConstructors, localMethods, localFields, finder);
                            }
                        }
                    }
                    catch (Exception e) {
                        log.debug("Unable to scan jar '{}'. {} ", new Object[]{url, e.getMessage()});
                    }
                }
            }
            catch ( Exception e ) {
                throw new RuntimeException("Classpath scanning failed for package \'" + basePackage + "\'", e);
            }
        }
    }
    
    private void applyFinderResults(Set<Class<?>> localClasses, Set<Constructor> localConstructors, Set<Method> localMethods, Set<Field> localFields, AnnotationFinder finder) {
        for (Class<?> cls : finder.getAnnotatedClasses()) {
            if (localClasses.contains(cls)) {
                log.debug(String.format("Duplicate class found for '%s'", cls.getCanonicalName()));
            }
            else {
                localClasses.add(cls);
            }
        }
        
        for (Method method : finder.getAnnotatedMethods()) {
            if (localMethods.contains(method)) {
                log.debug(String.format("Duplicate method found for '%s:%s'", method.getClass().getCanonicalName(), method.getName()));
            }
            else {
                localMethods.add(method);
            }
        }
        
        for (Constructor<?> ctor : finder.getAnnotatedConstructors()) {
            if (localConstructors.contains(ctor)) {
                log.debug(String.format("Duplicate constructor found for '%s:%s'", ctor.getClass().getCanonicalName(), ctor.toString()));
            }
            else {
                localConstructors.add(ctor);
            }
        }
        
        for (Field field : finder.getAnnotatedFields()) {
            if (localFields.contains(field)) {
                log.debug(String.format("Duplicate field found for '%s:%s'", field.getClass().getCanonicalName(), field.toString()));
            }
            else {
                localFields.add(field);
            }
        }
    }

    private boolean isJarURL(URL url) {
        String protocol = url.getProtocol();
        return "zip".equals(protocol) || "jar".equals(protocol) ||
                ("file".equals(protocol) && url.getPath().endsWith(".jar"));
    }
}
