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

/*
 * Based on work from the Proofpoint Platform published using the same Apache License, Version 2.0
 * https://github.com/proofpoint/platform
 */

package com.netflix.governator.lifecycle;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.netflix.governator.configuration.Configuration;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class LifecycleMethods
{
    private final Multimap<Class<? extends Annotation>, Method> methodMap = ArrayListMultimap.create();
    private final Multimap<Class<? extends Annotation>, Field> fieldMap = ArrayListMultimap.create();

    public LifecycleMethods(Class<?> clazz)
    {
        addLifeCycleMethods(clazz, new HashSet<String>(), new HashSet<String>(), new HashSet<String>());
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean hasFor(Class<? extends Annotation> annotation)
    {
        Collection<Method> methods = methodMap.get(annotation);
        if ( (methods != null) && (methods.size() > 0) )
        {
            return true;
        }

        Collection<Field> fields = fieldMap.get(annotation);
        if ( (fields != null) && (fields.size() > 0) )
        {
            return true;
        }

        return false;
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

    private void addLifeCycleMethods(Class<?> clazz, Set<String> usedConstructNames, Set<String> usedDestroyNames, Set<String> usedFieldNames)
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

            processField(field, Configuration.class, usedFieldNames);
        }

        for ( Method method : clazz.getDeclaredMethods() )
        {
            if ( method.isSynthetic() || method.isBridge() )
            {
                continue;
            }

            processMethod(method, PostConstruct.class, usedConstructNames);
            processMethod(method, PreDestroy.class, usedDestroyNames);
        }

        addLifeCycleMethods(clazz.getSuperclass(), usedConstructNames, usedDestroyNames, usedFieldNames);
        for ( Class<?> face : clazz.getInterfaces() )
        {
            addLifeCycleMethods(face, usedConstructNames, usedDestroyNames, usedFieldNames);
        }
    }

    private void processField(Field field, Class<Configuration> annotationClass, Set<String> usedSet)
    {
        if ( field.isAnnotationPresent(annotationClass) )
        {
            if ( !usedSet.contains(field.getName()) )
            {
                field.setAccessible(true);
                usedSet.add(field.getName());
                fieldMap.put(annotationClass, field);
            }
        }
    }

    private void processMethod(Method method, Class<? extends Annotation> annotationClass, Set<String> usedSet)
    {
        if ( method.isAnnotationPresent(annotationClass) )
        {
            if ( !usedSet.contains(method.getName()) )
            {
                if ( method.getParameterTypes().length != 0 )
                {
                    throw new UnsupportedOperationException(String.format("@PostConstruct/@PreDestroy methods cannot have arguments: %s", method.getDeclaringClass().getName() + "." + method.getName() + "(...)"));
                }

                method.setAccessible(true);
                usedSet.add(method.getName());
                methodMap.put(annotationClass, method);
            }
        }
    }
}