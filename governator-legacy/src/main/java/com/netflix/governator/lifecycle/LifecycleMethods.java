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

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.annotation.Resources;
import javax.validation.Constraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.ConfigurationVariable;
import com.netflix.governator.annotations.PreConfiguration;
import com.netflix.governator.annotations.WarmUp;

/**
 * Used internally to hold the methods important to the LifecycleManager
 */
public class LifecycleMethods {
    private static final Field[] EMPTY_FIELDS = new Field[] {};
    private static final Method[] EMPTY_METHODS = new Method[] {};
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Multimap<Class<? extends Annotation>, Field> fieldMap = ArrayListMultimap.create();
    private final Multimap<Class<? extends Annotation>, Method> methodMap = ArrayListMultimap.create();
    private final Multimap<Class<? extends Annotation>, Annotation> classMap = ArrayListMultimap.create();

    private boolean hasValidations = false;
    private final boolean hasResources;

    private static final Collection<Class<? extends Annotation>> fieldAnnotations;
    private static final Collection<Class<? extends Annotation>> methodAnnotations;
    private static final Collection<Class<? extends Annotation>> classAnnotations;

    static {
        ImmutableSet.Builder<Class<? extends Annotation>> methodAnnotationsBuilder = ImmutableSet.builder();
        methodAnnotationsBuilder.add(PreConfiguration.class);
        methodAnnotationsBuilder.add(PostConstruct.class);
        methodAnnotationsBuilder.add(PreDestroy.class);
        methodAnnotationsBuilder.add(Resource.class);
        methodAnnotationsBuilder.add(Resources.class);
        methodAnnotationsBuilder.add(WarmUp.class);
        methodAnnotations = methodAnnotationsBuilder.build();

        ImmutableSet.Builder<Class<? extends Annotation>> fieldAnnotationsBuilder = ImmutableSet.builder();
        fieldAnnotationsBuilder.add(Configuration.class);
        fieldAnnotationsBuilder.add(Resource.class);
        fieldAnnotationsBuilder.add(Resources.class);
        fieldAnnotationsBuilder.add(ConfigurationVariable.class);
        fieldAnnotations = fieldAnnotationsBuilder.build();

        ImmutableSet.Builder<Class<? extends Annotation>> classAnnotationsBuilder = ImmutableSet.builder();
        classAnnotationsBuilder.add(Resource.class);
        classAnnotationsBuilder.add(Resources.class);
        classAnnotations = classAnnotationsBuilder.build();
    }

    public LifecycleMethods(Class<?> clazz) {
        addLifeCycleMethods(clazz, ArrayListMultimap.<Class<? extends Annotation>, String> create());
        this.hasResources = fieldMap.containsKey(Resource.class) || 
                fieldMap.containsKey(Resources.class) ||
                methodMap.containsKey(Resource.class) ||
                methodMap.containsKey(Resources.class) ||
                classMap.containsKey(Resource.class) ||
                classMap.containsKey(Resources.class);
    }

    public boolean hasLifecycleAnnotations() {
        return hasValidations || !methodMap.isEmpty() || !fieldMap.isEmpty();
    }

    public boolean hasResources() {
        return hasResources;
    }

    public Collection<Method> methodsFor(Class<? extends Annotation> annotation) {
        Collection<Method> methods = methodMap.get(annotation);
        return (methods != null) ? methods : Collections.<Method>emptySet();
    }

    public Collection<Field> fieldsFor(Class<? extends Annotation> annotation) {
        Collection<Field> fields = fieldMap.get(annotation);
        return (fields != null) ? fields : Collections.<Field>emptySet();
    }

    @SuppressWarnings("unchecked")
    public <T extends Annotation> Collection<T> classAnnotationsFor(Class<T> annotation) {
        return (Collection<T>)classMap.get(annotation);
    }

    private void addLifeCycleMethods(Class<?> clazz, Multimap<Class<? extends Annotation>, String> usedNames) {
        if (clazz == null) {
            return;
        }

        for (Class<? extends Annotation> annotationClass : classAnnotations) {
            if (clazz.isAnnotationPresent(annotationClass)) {
                classMap.put(annotationClass, clazz.getAnnotation(annotationClass));
            }
        }

        for (Field field : getDeclardFields(clazz)) {
            if (field.isSynthetic()) {
                continue;
            }

            if (!hasValidations) {
                checkForValidations(field);
            }

            for (Class<? extends Annotation> annotationClass : fieldAnnotations) {
                processField(field, annotationClass, usedNames);
            }
        }

        for (Method method : getDeclaredMethods(clazz)) {
            if (method.isSynthetic() || method.isBridge()) {
                continue;
            }

            for (Class<? extends Annotation> annotationClass : methodAnnotations) {
                processMethod(method, annotationClass, usedNames);
            }
        }

        addLifeCycleMethods(clazz.getSuperclass(), usedNames);
        for (Class<?> face : clazz.getInterfaces()) {
            addLifeCycleMethods(face, usedNames);
        }
    }

    private Method[] getDeclaredMethods(Class<?> clazz) {
        try {
            return clazz.getDeclaredMethods();
        } catch (Throwable e) {
            handleReflectionError(clazz, e);
        }

        return EMPTY_METHODS;
    }

    private Field[] getDeclardFields(Class<?> clazz) {
        try {
            return clazz.getDeclaredFields();
        } catch (Throwable e) {
            handleReflectionError(clazz, e);
        }

        return EMPTY_FIELDS;
    }

    private void handleReflectionError(Class<?> clazz, Throwable e) {
        if (e != null) {
            if ((e instanceof NoClassDefFoundError) || (e instanceof ClassNotFoundException)) {
                log.debug(String.format(
                        "Class %s could not be resolved because of a class path error. Governator cannot further process the class.",
                        clazz.getName()), e);
                return;
            }

            handleReflectionError(clazz, e.getCause());
        }
    }

    private void checkForValidations(Field field) {
        for (Annotation annotation : field.getDeclaredAnnotations()) {
            if (annotation.annotationType().isAnnotationPresent(Constraint.class)) {
                hasValidations = true;
                break;
            }
        }
    }

    private void processField(Field field, Class<? extends Annotation> annotationClass,
            Multimap<Class<? extends Annotation>, String> usedNames) {
        if (field.isAnnotationPresent(annotationClass)) {
            if (!usedNames.get(annotationClass).contains(field.getName())) {
                field.setAccessible(true);
                usedNames.put(annotationClass, field.getName());
                fieldMap.put(annotationClass, field);
            }
        }
    }

    private void processMethod(Method method, Class<? extends Annotation> annotationClass,
            Multimap<Class<? extends Annotation>, String> usedNames) {
        if (method.isAnnotationPresent(annotationClass)) {
            if (!usedNames.get(annotationClass).contains(method.getName())) {
                method.setAccessible(true);
                usedNames.put(annotationClass, method.getName());
                methodMap.put(annotationClass, method);
            }
        }
    }
}
