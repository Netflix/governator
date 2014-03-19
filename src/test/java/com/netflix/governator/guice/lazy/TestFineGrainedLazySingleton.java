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

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Provider;
import com.netflix.governator.LifecycleInjectorBuilderProvider;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.guice.mocks.AnnotatedFineGrainedLazySingletonObject;
import com.netflix.governator.guice.mocks.LazySingletonObject;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;

public class TestFineGrainedLazySingleton extends LifecycleInjectorBuilderProvider
{
    public static class InjectedAnnotatedProvider
    {
        public final Provider<AnnotatedFineGrainedLazySingletonObject> provider;

        @Inject
        public InjectedAnnotatedProvider(Provider<AnnotatedFineGrainedLazySingletonObject> provider)
        {
            this.provider = provider;
        }
    }

    @BeforeMethod
    public void     setup()
    {
        AnnotatedFineGrainedLazySingletonObject.constructorCount.set(0);
        AnnotatedFineGrainedLazySingletonObject.postConstructCount.set(0);
        LazySingletonObject.constructorCount.set(0);
        LazySingletonObject.postConstructCount.set(0);
    }

    @FineGrainedLazySingleton
    public static class DeadLockTester
    {
        @Inject
        public DeadLockTester(final Injector injector) throws InterruptedException
        {
            final CountDownLatch        latch = new CountDownLatch(1);
            Executors.newSingleThreadExecutor().submit
            (
                new Callable<Object>()
                {
                    @Override
                    public Object call() throws Exception
                    {
                        injector.getInstance(AnnotatedFineGrainedLazySingletonObject.class);
                        latch.countDown();
                        return null;
                    }
                }
            );
            latch.await();
        }
    }

    @Test
    public void     testDeadLock() throws InterruptedException
    {
        AbstractModule      module = new AbstractModule()
        {
            @Override
            protected void configure()
            {
                binder().bindScope(LazySingleton.class, LazySingletonScope.get());
                binder().bindScope(FineGrainedLazySingleton.class, FineGrainedLazySingletonScope.get());
            }
        };
        Injector            injector = Guice.createInjector(module);
        injector.getInstance(DeadLockTester.class); // if FineGrainedLazySingleton is not used, this line will deadlock
        Assert.assertEquals(AnnotatedFineGrainedLazySingletonObject.constructorCount.get(), 1);
    }

    @Test(dataProvider = "builders")
    public void testUsingAnnotation(LifecycleInjectorBuilder lifecycleInjectorBuilder)
    {
        Injector    injector = lifecycleInjectorBuilder.createInjector();

        Assert.assertEquals(AnnotatedFineGrainedLazySingletonObject.constructorCount.get(), 0);
        Assert.assertEquals(AnnotatedFineGrainedLazySingletonObject.postConstructCount.get(), 0);

        AnnotatedFineGrainedLazySingletonObject instance = injector.getInstance(AnnotatedFineGrainedLazySingletonObject.class);
        Assert.assertEquals(AnnotatedFineGrainedLazySingletonObject.constructorCount.get(), 1);
        Assert.assertEquals(AnnotatedFineGrainedLazySingletonObject.postConstructCount.get(), 1);

        AnnotatedFineGrainedLazySingletonObject instance2 = injector.getInstance(AnnotatedFineGrainedLazySingletonObject.class);
        Assert.assertEquals(AnnotatedFineGrainedLazySingletonObject.constructorCount.get(), 1);
        Assert.assertEquals(AnnotatedFineGrainedLazySingletonObject.postConstructCount.get(), 1);

        Assert.assertSame(instance, instance2);
    }

    @Test(dataProvider = "builders")
    public void testUsingInWithProviderAndAnnotation(LifecycleInjectorBuilder lifecycleInjectorBuilder)
    {
        Injector    injector = lifecycleInjectorBuilder.createInjector();

        Assert.assertEquals(AnnotatedFineGrainedLazySingletonObject.constructorCount.get(), 0);
        Assert.assertEquals(AnnotatedFineGrainedLazySingletonObject.postConstructCount.get(), 0);

        InjectedAnnotatedProvider injectedProvider = injector.getInstance(InjectedAnnotatedProvider.class);
        Assert.assertEquals(AnnotatedFineGrainedLazySingletonObject.constructorCount.get(), 0);
        Assert.assertEquals(AnnotatedFineGrainedLazySingletonObject.postConstructCount.get(), 0);

        AnnotatedFineGrainedLazySingletonObject instance = injectedProvider.provider.get();
        Assert.assertEquals(AnnotatedFineGrainedLazySingletonObject.constructorCount.get(), 1);
        Assert.assertEquals(AnnotatedFineGrainedLazySingletonObject.postConstructCount.get(), 1);

        AnnotatedFineGrainedLazySingletonObject instance2 = injectedProvider.provider.get();
        Assert.assertEquals(AnnotatedFineGrainedLazySingletonObject.constructorCount.get(), 1);
        Assert.assertEquals(AnnotatedFineGrainedLazySingletonObject.postConstructCount.get(), 1);

        Assert.assertSame(instance, instance2);
    }
}
