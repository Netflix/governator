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

import com.google.common.base.Function;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Collections2;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;

/**
 * Used internally to hold the methods important to the LifecycleManager
 */
public class LifecycleMethods {
    final Multimap<Class<? extends Annotation>, Field> fieldMap = ArrayListMultimap.create();
    final Multimap<Class<? extends Annotation>, Method> methodMap = ArrayListMultimap.create();
    final Multimap<Class<? extends Annotation>, Annotation> classMap = ArrayListMultimap.create();

    boolean hasValidations = false;

    public boolean hasLifecycleAnnotations() {
        return hasValidations || (methodMap.size() > 0) || (fieldMap.size() > 0);
    }

    /**
     * Return the collection of all methods with a specific annotation
     * @param annotation
     * @return
     */
    public Collection<Method> methodsFor(Class<? extends Annotation> annotation) {
        Collection<Method> methods = methodMap.get(annotation);
        return (methods != null) ? methods : Lists.<Method>newArrayList();
    }

    /**
     * Return the collection of all fields with a specific annotation
     * @param annotation
     * @return
     */
    public Collection<Field> fieldsFor(Class<? extends Annotation> annotation) {
        Collection<Field> fields = fieldMap.get(annotation);
        return (fields != null) ? fields : Lists.<Field>newArrayList();
    }

    /**
     * Return the collection of all class annotations of a specific type
     * @param annotation
     * @return
     */
    public <T extends Annotation> Collection<T> classAnnotationsFor(Class<T> annotation) {
        Collection<Annotation> annotations = classMap.get(annotation);
        return Collections2.transform
        (
            annotations,
            new Function<Annotation, T>()
            {
                @Override
                public T apply(Annotation annotation)
                {
                    //noinspection unchecked
                    return (T)annotation;
                }
            }
        );
    }



}