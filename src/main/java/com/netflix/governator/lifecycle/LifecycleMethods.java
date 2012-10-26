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

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.PreConfiguration;
import com.netflix.governator.annotations.WarmUp;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.validation.Constraint;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;

/**
 * Used internally to hold the methods important to the LifecycleManager
 */
public class LifecycleMethods
{
    private final Multimap<Class<? extends Annotation>, Method> methodMap = ArrayListMultimap.create();
    private final Multimap<Class<? extends Annotation>, Field> fieldMap = ArrayListMultimap.create();

    private boolean hasValidations = false;

    private static final Collection<Class<? extends Annotation>>    methodAnnotations = ImmutableSet.of
    (
        PreConfiguration.class,
        PostConstruct.class,
        PreDestroy.class,
        WarmUp.class
    );

    public LifecycleMethods(Class<?> clazz)
    {
        addLifeCycleMethods(clazz, ArrayListMultimap.<Class<? extends Annotation>, String>create());
    }

    public boolean      hasLifecycleAnnotations()
    {
        return hasValidations || (methodMap.size() > 0) || (fieldMap.size() > 0);
    }

    public Collection<Method> methodsFor(Class<? extends Annotation> annotation)
    {
        Collection<Method> methods = methodMap.get(annotation);
        return (methods != null) ? methods : Lists.<Method>newArrayList();
    }

    public Collection<Field> fieldsFor(Class<? extends Annotation> annotation)
    {
        Collection<Field> fields = fieldMap.get(annotation);
        return (fields != null) ? fields : Lists.<Field>newArrayList();
    }

    private void addLifeCycleMethods(Class<?> clazz, Multimap<Class<? extends Annotation>, String> usedNames)
    {
        if ( clazz == null )
        {
            return;
        }

        for ( Field field : clazz.getDeclaredFields() )
        {
            if ( field.isSynthetic() )
            {
                continue;
            }

            if ( !hasValidations )
            {
                checkForValidations(field);
            }

            processField(field, Configuration.class, usedNames);
        }

        for ( Method method : clazz.getDeclaredMethods() )
        {
            if ( method.isSynthetic() || method.isBridge() )
            {
                continue;
            }

            for ( Class<? extends Annotation> annotationClass : methodAnnotations )
            {
                processMethod(method, annotationClass, usedNames);
            }
        }

        addLifeCycleMethods(clazz.getSuperclass(), usedNames);
        for ( Class<?> face : clazz.getInterfaces() )
        {
            addLifeCycleMethods(face, usedNames);
        }
    }

    private void checkForValidations(Field field)
    {
        for ( Annotation annotation : field.getDeclaredAnnotations() )
        {
            if ( annotation.annotationType().isAnnotationPresent(Constraint.class) )
            {
                hasValidations = true;
                break;
            }
        }
    }

    private void processField(Field field, Class<? extends Annotation> annotationClass, Multimap<Class<? extends Annotation>, String> usedNames)
    {
        if ( field.isAnnotationPresent(annotationClass) )
        {
            if ( !usedNames.get(annotationClass).contains(field.getName()) )
            {
                field.setAccessible(true);
                usedNames.put(annotationClass, field.getName());
                fieldMap.put(annotationClass, field);
            }
        }
    }

    private void processMethod(Method method, Class<? extends Annotation> annotationClass, Multimap<Class<? extends Annotation>, String> usedNames)
    {
        if ( method.isAnnotationPresent(annotationClass) )
        {
            if ( !usedNames.get(annotationClass).contains(method.getName()) )
            {
                if ( method.getParameterTypes().length != 0 )
                {
                    throw new UnsupportedOperationException(String.format("@PostConstruct/@PreDestroy methods cannot have arguments: %s", method.getDeclaringClass().getName() + "." + method.getName() + "(...)"));
                }

                method.setAccessible(true);
                usedNames.put(annotationClass, method.getName());
                methodMap.put(annotationClass, method);
            }
        }
    }
}