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
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    private static final Lookup METHOD_HANDLE_LOOKUP = MethodHandles.lookup();
    private static final ConcurrentMap<Object, MethodHandle> methodHandlesMap = new ConcurrentHashMap<>(1<<13, 0.75f);

    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Multimap<Class<? extends Annotation>, Field> fieldMap = ArrayListMultimap.create(1<<13, 1<<3);
    private final Multimap<Class<? extends Annotation>, Method> methodMap = ArrayListMultimap.create(1<<13, 1<<3);
    private final Multimap<Class<? extends Annotation>, Annotation> classMap = ArrayListMultimap.create(1<<13, 1<<3);

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

        for (Field field : getDeclaredFields(clazz)) {
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

    private Field[] getDeclaredFields(Class<?> clazz) {
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
        this.hasValidations =field.getAnnotationsByType(Constraint.class).length > 0;
    }

    private void processField(Field field, Class<? extends Annotation> annotationClass,
            Multimap<Class<? extends Annotation>, String> usedNames) {
        if (field.isAnnotationPresent(annotationClass)) {
            String fieldName = field.getName();
            if (!usedNames.get(annotationClass).contains(fieldName)) {
                field.setAccessible(true);
                usedNames.put(annotationClass, fieldName);
                fieldMap.put(annotationClass, field);
            }
        }
    }

    private void processMethod(Method method, Class<? extends Annotation> annotationClass,
            Multimap<Class<? extends Annotation>, String> usedNames) {
        if (method.isAnnotationPresent(annotationClass)) {
            String methodName = method.getName();
            if (!usedNames.get(annotationClass).contains(methodName)) {
                method.setAccessible(true);
                usedNames.put(annotationClass, methodName);
                methodMap.put(annotationClass, method);
                try {
                    methodHandlesMap.put(annotationClass, METHOD_HANDLE_LOOKUP.unreflect(method));
                } catch (IllegalAccessException e) {
                    // that's ok, will use reflected method
                }
            }
        }
    }
    
    public void methodInvoke(Class<? extends Annotation> annotation, Object obj) throws Exception {
        if  (methodMap.containsKey(annotation)) {
           for (Method m : methodMap.get(annotation)) {
               methodInvoke(m, obj);
           }
        }

    }
    
    public static void methodInvoke(Method method, Object target) throws InvocationTargetException, IllegalAccessException {
        try {
            MethodHandle handler = methodHandlesMap.get(method);
            if (handler == null) {
                handler = METHOD_HANDLE_LOOKUP.unreflect(method);
                methodHandlesMap.put(method, handler);
            }            
            
            try {
                handler.invoke(target);
            } catch (Throwable e) {
                throw new InvocationTargetException(e, "invokedynamic");
            }
        } catch (IllegalAccessException e) {
            // fall back to reflected invocation
            method.invoke(target);
            return;
        }        
    }

    public static <T> T fieldGet(Field variableField, Object obj) throws InvocationTargetException, IllegalAccessException {
        MethodHandle handler = METHOD_HANDLE_LOOKUP.unreflectGetter(variableField);
        try {
            return (T)handler.invoke(obj);
        } catch (Throwable e) {
            throw new InvocationTargetException(e);
        }
    }

    public static <T> T fieldSet(Field field, Object object, Object value) throws InvocationTargetException, IllegalAccessException {
        MethodHandle handler = methodHandlesMap.get(field);
        if (handler == null) {
            handler = METHOD_HANDLE_LOOKUP.unreflectSetter(field);
            methodHandlesMap.put(field, handler);
        }
        try {
            return (T)handler.invoke(object, value);
        } catch (Throwable e) {
            throw new InvocationTargetException(e);
        }
    }

}
