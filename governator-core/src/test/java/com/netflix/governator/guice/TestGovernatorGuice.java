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

import java.util.List;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.netflix.governator.LifecycleInjectorBuilderProvider;
import com.netflix.governator.guice.mocks.ObjectWithGenericInterface;
import com.netflix.governator.guice.mocks.SimpleContainer;
import com.netflix.governator.guice.mocks.SimpleEagerSingleton;
import com.netflix.governator.guice.mocks.SimpleGenericInterface;
import com.netflix.governator.guice.mocks.SimpleInterface;
import com.netflix.governator.guice.mocks.SimplePojo;
import com.netflix.governator.guice.mocks.SimplePojoAlt;
import com.netflix.governator.guice.mocks.SimpleProvider;
import com.netflix.governator.guice.mocks.SimpleProviderAlt;
import com.netflix.governator.guice.mocks.SimpleSingleton;
import com.netflix.governator.guice.mocks.UnreferencedSingleton;
import com.netflix.governator.guice.modules.ObjectA;
import com.netflix.governator.guice.modules.ObjectB;
import com.netflix.governator.lifecycle.DefaultLifecycleListener;
import com.netflix.governator.lifecycle.FilteredLifecycleListener;
import com.netflix.governator.lifecycle.LifecycleListener;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.LifecycleState;

public class TestGovernatorGuice extends LifecycleInjectorBuilderProvider
{
    private static final String PACKAGES = "com.netflix.governator.guice.mocks";

    @Test(dataProvider = "builders")
    public void     testAutoBindSingletonToGenericInterface(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        Injector    injector = lifecycleInjectorBuilder
            .usingBasePackages("com.netflix.governator.guice.mocks")
            .createInjector();

        Key<SimpleGenericInterface<String>>     key = Key.get(new TypeLiteral<SimpleGenericInterface<String>>(){});
        SimpleGenericInterface<String>          simple = injector.getInstance(key);

        Assert.assertEquals(simple.getValue(), "a is a");

        ObjectWithGenericInterface              obj = injector.getInstance(ObjectWithGenericInterface.class);
        Assert.assertEquals(obj.getObj().getValue(), "a is a");
    }

    @Test(dataProvider = "builders")
    public void     testAutoBindSingletonToInterface(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        Injector    injector = lifecycleInjectorBuilder
            .usingBasePackages("com.netflix.governator.guice.mocks")
            .createInjector();
        SimpleInterface     simple = injector.getInstance(SimpleInterface.class);

        Assert.assertEquals(simple.getValue(), 1234);
    }

    @Test(dataProvider = "builders")
    public void     testAutoBindModules(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        Injector    injector = lifecycleInjectorBuilder
            .usingBasePackages("com.netflix.governator.guice.modules")
            .createInjector();
        ObjectA objectA = injector.getInstance(ObjectA.class);
        ObjectB objectB = injector.getInstance(ObjectB.class);

        Assert.assertEquals(objectA.getColor(), "blue");
        Assert.assertEquals(objectB.getSize(), "large");
    }

    @Test(dataProvider = "builders")
    public void     testAutoBindSingletonVsSingleton(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        final List<Object>        objects = Lists.newArrayList();
        final LifecycleListener   listener = new DefaultLifecycleListener()
        {
            @Override
            public <T> void objectInjected(TypeLiteral<T> type, T obj)
            {
                objects.add(obj);
            }

            @Override
            public void stateChanged(Object obj, LifecycleState newState)
            {
            }
        };
        Injector    injector = lifecycleInjectorBuilder
            .usingBasePackages(PACKAGES)
            .withBootstrapModule
            (
                new BootstrapModule()
                {
                    @Override
                    public void configure(BootstrapBinder binder)
                    {
                        binder.bindLifecycleListener().toInstance(new FilteredLifecycleListener(listener, PACKAGES));
                    }
                }
            )
            .createInjector();

        Assert.assertNull
        (
            Iterables.find
            (
                objects,
                new Predicate<Object>()
                {
                    @Override
                    public boolean apply(Object obj)
                    {
                        return obj instanceof UnreferencedSingleton;
                    }
                },
                null
            )
        );
        Assert.assertNotNull
        (
            Iterables.find
                (
                    objects,
                    new Predicate<Object>()
                    {
                        @Override
                        public boolean apply(Object obj)
                        {
                            return obj instanceof SimpleEagerSingleton;
                        }
                    },
                    null
                )
        );

        injector.getInstance(UnreferencedSingleton.class);
        Assert.assertNotNull
        (
            Iterables.find
                (
                    objects,
                    new Predicate<Object>()
                    {
                        @Override
                        public boolean apply(Object obj)
                        {
                            return obj instanceof UnreferencedSingleton;
                        }
                    },
                    null
                )
        );
    }

    @Test
    public void     testSimpleProvider() throws Exception
    {
        Injector                injector = Guice.createInjector
        (
            new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    ProviderBinderUtil.bind(binder(), SimpleProvider.class, Scopes.SINGLETON);
                    ProviderBinderUtil.bind(binder(), SimpleProviderAlt.class, Scopes.SINGLETON);
                }
            }
        );

        SimplePojo pojo = injector.getInstance(SimplePojo.class);
        Assert.assertEquals(pojo.getI(), 1);
        Assert.assertEquals(pojo.getS(), "one");

        SimplePojoAlt pojoAlt = injector.getInstance(SimplePojoAlt.class);
        Assert.assertEquals(pojoAlt.getL(), 3);
        Assert.assertEquals(pojoAlt.getD(), 4.5);
    }

    @Test(dataProvider = "builders")
    public void     testSimpleSingleton(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        Injector            injector = lifecycleInjectorBuilder.usingBasePackages(PACKAGES).createInjector();
        LifecycleManager    manager = injector.getInstance(LifecycleManager.class);
        manager.start();

        SimpleSingleton     instance = injector.getInstance(SimpleSingleton.class);

        Assert.assertEquals(instance.startCount.get(), 1);
        Assert.assertEquals(instance.finishCount.get(), 0);

        manager.close();

        Assert.assertEquals(instance.startCount.get(), 1);
        Assert.assertEquals(instance.finishCount.get(), 1);
    }

    @Test(dataProvider = "builders")
    public void     testInjectedIntoAnother(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        Injector            injector = lifecycleInjectorBuilder.usingBasePackages(PACKAGES).createInjector();
        LifecycleManager    manager = injector.getInstance(LifecycleManager.class);
        manager.start();

        SimpleContainer     instance = injector.getInstance(SimpleContainer.class);

        Assert.assertEquals(instance.simpleObject.startCount.get(), 1);
        Assert.assertEquals(instance.simpleObject.finishCount.get(), 0);

        manager.close();

        Assert.assertEquals(instance.simpleObject.startCount.get(), 1);
        Assert.assertEquals(instance.simpleObject.finishCount.get(), 1);
    }
}
