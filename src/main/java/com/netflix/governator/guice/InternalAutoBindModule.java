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

package com.netflix.governator.guice;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Types;
import com.netflix.governator.annotations.AutoBind;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.lifecycle.ClasspathScanner;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.List;

class InternalAutoBindModule extends AbstractModule
{
    private final List<Class<?>> ignoreClasses;
    private final Injector injector;
    private final ClasspathScanner classpathScanner;

    InternalAutoBindModule(Injector injector, ClasspathScanner classpathScanner, Collection<Class<?>> ignoreClasses)
    {
        this.injector = injector;
        this.classpathScanner = classpathScanner;
        Preconditions.checkNotNull(ignoreClasses, "ignoreClasses cannot be null");

        this.ignoreClasses = ImmutableList.copyOf(ignoreClasses);
    }

    @Override
    protected void configure()
    {
        bindAutoBindSingletons();
        bindAutoBindConstructors();
        bindAutoBindMethods();
        bindAutoBindFields();
    }

    private void bindAutoBindFields()
    {
        for ( Field field : classpathScanner.getFields() )
        {
            if ( ignoreClasses.contains(field.getDeclaringClass()) )
            {
                continue;
            }

            bindAnnotations(field.getDeclaredAnnotations());
        }
    }

    private void bindAutoBindMethods()
    {
        for ( Method method : classpathScanner.getMethods() )
        {
            if ( ignoreClasses.contains(method.getDeclaringClass()) )
            {
                continue;
            }

            bindParameterAnnotations(method.getParameterAnnotations());
        }
    }

    private void bindAutoBindConstructors()
    {
        for ( Constructor constructor : classpathScanner.getConstructors() )
        {
            if ( ignoreClasses.contains(constructor.getDeclaringClass()) )
            {
                continue;
            }

            bindParameterAnnotations(constructor.getParameterAnnotations());
        }
    }

    private void bindParameterAnnotations(Annotation[][] parameterAnnotations)
    {
        for ( Annotation[] annotations : parameterAnnotations )
        {
            bindAnnotations(annotations);
        }
    }

    private void bindAnnotations(Annotation[] annotations)
    {
        for ( Annotation annotation : annotations )
        {
            AutoBindProvider autoBindProvider = getAutoBindProvider(annotation);
            if ( autoBindProvider != null )
            {
                //noinspection unchecked
                autoBindProvider.configure(binder(), annotation);
            }
        }
    }

    private AutoBindProvider getAutoBindProvider(Annotation annotation)
    {
        AutoBindProvider  autoBindProvider = null;
        if ( annotation.annotationType().isAnnotationPresent(AutoBind.class) )
        {
            ParameterizedType parameterizedType = Types.newParameterizedType(AutoBindProvider.class, annotation.annotationType());
            autoBindProvider = (AutoBindProvider<?>)injector.getInstance(Key.get(TypeLiteral.get(parameterizedType)));
        }
        else if ( annotation.annotationType().isAssignableFrom(AutoBind.class) )
        {
            autoBindProvider = injector.getInstance(Key.get(new TypeLiteral<AutoBindProvider<AutoBind>>(){}));
        }
        return autoBindProvider;
    }

    @SuppressWarnings("unchecked")
    private void bindAutoBindSingletons()
    {
        for ( Class<?> clazz : classpathScanner.getClasses() )
        {
            if ( ignoreClasses.contains(clazz) || !clazz.isAnnotationPresent(AutoBindSingleton.class) )
            {
                continue;
            }

            if ( javax.inject.Provider.class.isAssignableFrom(clazz) )
            {
                ProviderBinderUtil.bind(binder(), (Class<? extends javax.inject.Provider>)clazz, Scopes.SINGLETON);
            }
            else
            {
                binder().bind(clazz).asEagerSingleton();
            }
        }
    }
}
