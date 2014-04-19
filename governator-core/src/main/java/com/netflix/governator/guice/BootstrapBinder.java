/*
 * Copyright 2013 Netflix, Inc.
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

import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Module;
import com.google.inject.PrivateBinder;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.AnnotatedConstantBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.matcher.Matcher;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.Message;
import com.google.inject.spi.TypeConverter;
import com.google.inject.spi.TypeListener;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.lifecycle.LifecycleListener;
import com.netflix.governator.lifecycle.ResourceLocator;
import org.aopalliance.intercept.MethodInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Set;

public class BootstrapBinder implements Binder
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final Binder binder;
    private final Set<Key<?>> boundKeys = Sets.newHashSet();
    private final Set<Class<?>> boundClasses = Sets.newHashSet();

    @Override
    public void bindInterceptor(Matcher<? super Class<?>> classMatcher, Matcher<? super Method> methodMatcher, MethodInterceptor... interceptors)
    {
        binder.bindInterceptor(classMatcher, methodMatcher, interceptors);
    }

    @Override
    public void bindScope(Class<? extends Annotation> annotationType, Scope scope)
    {
        binder.bindScope(annotationType, scope);
    }

    /**
     * Use this to bind a {@link LifecycleListener}. It internally uses a Multibinder to do the
     * binding so that you can bind multiple LifecycleListeners
     *
     * @return a binding builder used to add a new element in the set.
     */
    public LinkedBindingBuilder<LifecycleListener> bindLifecycleListener()
    {
        return Multibinder.newSetBinder(binder, LifecycleListener.class).addBinding();
    }

    /**
     * Use this to bind a {@link ResourceLocator}. It internally uses a Multibinder to do the
     * binding so that you can bind multiple ResourceLocators
     *
     * @return a binding builder used to add a new element in the set.
     */
    public LinkedBindingBuilder<ResourceLocator> bindResourceLocator()
    {
        return Multibinder.newSetBinder(binder, ResourceLocator.class).addBinding();
    }

    /**
     * Use this to bind {@link ConfigurationProvider}s. Do NOT use standard Guice binding.
     *
     * @return configuration binding builder
     */
    public LinkedBindingBuilder<ConfigurationProvider> bindConfigurationProvider()
    {
        return Multibinder.newSetBinder(binder, ConfigurationProvider.class).addBinding();
    }

    @Override
    public <T> LinkedBindingBuilder<T> bind(Key<T> key)
    {
        boundKeys.add(key);
        warnOnSpecialized(key.getTypeLiteral().getRawType());
        return binder.bind(key);
    }

    @Override
    public <T> AnnotatedBindingBuilder<T> bind(TypeLiteral<T> typeLiteral)
    {
        boundKeys.add(Key.get(typeLiteral));
        warnOnSpecialized(typeLiteral.getRawType());
        return binder.bind(typeLiteral);
    }

    @Override
    public <T> AnnotatedBindingBuilder<T> bind(Class<T> type)
    {
        boundClasses.add(type);
        warnOnSpecialized(type);
        return binder.bind(type);
    }

    @Override
    public AnnotatedConstantBindingBuilder bindConstant()
    {
        return binder.bindConstant();
    }

    @Override
    public <T> void requestInjection(TypeLiteral<T> type, T instance)
    {
        binder.requestInjection(type, instance);
    }

    @Override
    public void requestInjection(Object instance)
    {
        binder.requestInjection(instance);
    }

    @Override
    public void requestStaticInjection(Class<?>... types)
    {
        binder.requestStaticInjection(types);
    }

    @Override
    public void install(Module module)
    {
        binder.install(module);
    }

    @Override
    public Stage currentStage()
    {
        return binder.currentStage();
    }

    @Override
    public void addError(String message, Object... arguments)
    {
        binder.addError(message, arguments);
    }

    @Override
    public void addError(Throwable t)
    {
        binder.addError(t);
    }

    @Override
    public void addError(Message message)
    {
        binder.addError(message);
    }

    @Override
    public <T> Provider<T> getProvider(Key<T> key)
    {
        return binder.getProvider(key);
    }

    @Override
    public <T> Provider<T> getProvider(Class<T> type)
    {
        return binder.getProvider(type);
    }

    @Override
    public <T> MembersInjector<T> getMembersInjector(TypeLiteral<T> typeLiteral)
    {
        return binder.getMembersInjector(typeLiteral);
    }

    @Override
    public <T> MembersInjector<T> getMembersInjector(Class<T> type)
    {
        return binder.getMembersInjector(type);
    }

    @Override
    public void convertToTypes(Matcher<? super TypeLiteral<?>> typeMatcher, TypeConverter converter)
    {
        binder.convertToTypes(typeMatcher, converter);
    }

    @Override
    public void bindListener(Matcher<? super TypeLiteral<?>> typeMatcher, TypeListener listener)
    {
        binder.bindListener(typeMatcher, listener);
    }

    @Override
    public Binder withSource(Object source)
    {
        return binder.withSource(source);
    }

    @Override
    public Binder skipSources(Class... classesToSkip)
    {
        return binder.skipSources(classesToSkip);
    }

    @Override
    public PrivateBinder newPrivateBinder()
    {
        return binder.newPrivateBinder();
    }

    @Override
    public void requireExplicitBindings()
    {
        binder.requireExplicitBindings();
    }

    @Override
    public void disableCircularProxies()
    {
        binder.disableCircularProxies();
    }

    BootstrapBinder(Binder binder)
    {
        this.binder = binder;
    }

    Set<Key<?>> getBoundKeys()
    {
        return boundKeys;
    }

    Set<Class<?>> getBoundClasses()
    {
        return boundClasses;
    }

    private<T> void    warnOnSpecialized(Class<T> clazz)
    {
        if ( ConfigurationProvider.class.isAssignableFrom(clazz) )
        {
            log.warn("You should use the specialized binding method for ConfigurationProviders");
        }
        if ( LifecycleListener.class.isAssignableFrom(clazz) )
        {
            log.warn("You should use the specialized binding method for LifecycleListener");
        }
        if ( ResourceLocator.class.isAssignableFrom(clazz) )
        {
            log.warn("You should use the specialized binding method for ResourceLocator");
        }
    }
}
