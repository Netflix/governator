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

package com.netflix.governator.guice.lazy;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.netflix.governator.LifecycleInjectorBuilderProvider;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.guice.mocks.AnnotatedLazySingletonObject;
import com.netflix.governator.guice.mocks.LazySingletonObject;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestLazySingleton extends LifecycleInjectorBuilderProvider
{
    public static class InjectedProvider
    {
        public final Provider<LazySingletonObject> provider;

        @Inject
        public InjectedProvider(Provider<LazySingletonObject> provider)
        {
            this.provider = provider;
        }
    }

    public static class InjectedAnnotatedProvider
    {
        public final Provider<AnnotatedLazySingletonObject> provider;

        @Inject
        public InjectedAnnotatedProvider(Provider<AnnotatedLazySingletonObject> provider)
        {
            this.provider = provider;
        }
    }

    @BeforeMethod
    public void     setup()
    {
        AnnotatedLazySingletonObject.constructorCount.set(0);
        AnnotatedLazySingletonObject.postConstructCount.set(0);
        LazySingletonObject.constructorCount.set(0);
        LazySingletonObject.postConstructCount.set(0);
    }

    @Test(dataProvider = "builders")
    public void testUsingAnnotation(LifecycleInjectorBuilder lifecycleInjectorBuilder)
    {
        Injector    injector = lifecycleInjectorBuilder.createInjector();

        Assert.assertEquals(AnnotatedLazySingletonObject.constructorCount.get(), 0);
        Assert.assertEquals(AnnotatedLazySingletonObject.postConstructCount.get(), 0);

        AnnotatedLazySingletonObject instance = injector.getInstance(AnnotatedLazySingletonObject.class);
        Assert.assertEquals(AnnotatedLazySingletonObject.constructorCount.get(), 1);
        Assert.assertEquals(AnnotatedLazySingletonObject.postConstructCount.get(), 1);

        AnnotatedLazySingletonObject instance2 = injector.getInstance(AnnotatedLazySingletonObject.class);
        Assert.assertEquals(AnnotatedLazySingletonObject.constructorCount.get(), 1);
        Assert.assertEquals(AnnotatedLazySingletonObject.postConstructCount.get(), 1);

        Assert.assertSame(instance, instance2);
    }

    @Test(dataProvider = "builders")
    public void testUsingInWithProviderAndAnnotation(LifecycleInjectorBuilder lifecycleInjectorBuilder)
    {
        Injector    injector = lifecycleInjectorBuilder.createInjector();

        Assert.assertEquals(AnnotatedLazySingletonObject.constructorCount.get(), 0);
        Assert.assertEquals(AnnotatedLazySingletonObject.postConstructCount.get(), 0);

        InjectedAnnotatedProvider injectedProvider = injector.getInstance(InjectedAnnotatedProvider.class);
        Assert.assertEquals(AnnotatedLazySingletonObject.constructorCount.get(), 0);
        Assert.assertEquals(AnnotatedLazySingletonObject.postConstructCount.get(), 0);

        AnnotatedLazySingletonObject instance = injectedProvider.provider.get();
        Assert.assertEquals(AnnotatedLazySingletonObject.constructorCount.get(), 1);
        Assert.assertEquals(AnnotatedLazySingletonObject.postConstructCount.get(), 1);

        AnnotatedLazySingletonObject instance2 = injectedProvider.provider.get();
        Assert.assertEquals(AnnotatedLazySingletonObject.constructorCount.get(), 1);
        Assert.assertEquals(AnnotatedLazySingletonObject.postConstructCount.get(), 1);

        Assert.assertSame(instance, instance2);
    }

    @Test(dataProvider = "builders")
    public void testUsingIn(LifecycleInjectorBuilder lifecycleInjectorBuilder)
    {
        Injector    injector = lifecycleInjectorBuilder
            .withModules
            (
                new Module()
                {
                    @Override
                    public void configure(Binder binder)
                    {
                        binder.bind(LazySingletonObject.class).in(LazySingletonScope.get());
                    }
                }
            )
            .createInjector();

        Assert.assertEquals(LazySingletonObject.constructorCount.get(), 0);
        Assert.assertEquals(LazySingletonObject.postConstructCount.get(), 0);

        LazySingletonObject instance = injector.getInstance(LazySingletonObject.class);
        Assert.assertEquals(LazySingletonObject.constructorCount.get(), 1);
        Assert.assertEquals(LazySingletonObject.postConstructCount.get(), 1);

        LazySingletonObject instance2 = injector.getInstance(LazySingletonObject.class);
        Assert.assertEquals(LazySingletonObject.constructorCount.get(), 1);
        Assert.assertEquals(LazySingletonObject.postConstructCount.get(), 1);

        Assert.assertSame(instance, instance2);
    }

    @Test(dataProvider = "builders")
    public void testUsingInWithProvider(LifecycleInjectorBuilder lifecycleInjectorBuilder)
    {
        Injector    injector = lifecycleInjectorBuilder
            .withModules
            (
                new Module()
                {
                    @Override
                    public void configure(Binder binder)
                    {
                        binder.bind(LazySingletonObject.class).in(LazySingletonScope.get());
                    }
                }
            )
            .createInjector();

        Assert.assertEquals(LazySingletonObject.constructorCount.get(), 0);
        Assert.assertEquals(LazySingletonObject.postConstructCount.get(), 0);

        InjectedProvider injectedProvider = injector.getInstance(InjectedProvider.class);
        Assert.assertEquals(LazySingletonObject.constructorCount.get(), 0);
        Assert.assertEquals(LazySingletonObject.postConstructCount.get(), 0);

        LazySingletonObject instance = injectedProvider.provider.get();
        Assert.assertEquals(LazySingletonObject.constructorCount.get(), 1);
        Assert.assertEquals(LazySingletonObject.postConstructCount.get(), 1);

        LazySingletonObject instance2 = injectedProvider.provider.get();
        Assert.assertEquals(LazySingletonObject.constructorCount.get(), 1);
        Assert.assertEquals(LazySingletonObject.postConstructCount.get(), 1);

        Assert.assertSame(instance, instance2);
    }
}
