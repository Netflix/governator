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

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.netflix.governator.guice.mocks.SimpleContainer;
import com.netflix.governator.guice.mocks.SimpleEagerSingleton;
import com.netflix.governator.guice.mocks.SimplePojo;
import com.netflix.governator.guice.mocks.SimplePojoAlt;
import com.netflix.governator.guice.mocks.SimpleProvider;
import com.netflix.governator.guice.mocks.SimpleProviderAlt;
import com.netflix.governator.guice.mocks.SimpleSingleton;
import com.netflix.governator.guice.mocks.UnreferencedSingleton;
import com.netflix.governator.lifecycle.FilteredLifecycleListener;
import com.netflix.governator.lifecycle.LifecycleListener;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.LifecycleState;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.List;

public class TestGovernatorGuice
{
    private static final String PACKAGES = "com.netflix.governator.guice.mocks";

    @Test
    public void     testAutoBindSingletonVsSingleton() throws Exception
    {
        final List<Object>        objects = Lists.newArrayList();
        final LifecycleListener   listener = new LifecycleListener()
        {
            @Override
            public void objectInjected(Object obj)
            {
                objects.add(obj);
            }

            @Override
            public void stateChanged(Object obj, LifecycleState newState)
            {
            }
        };
        Injector    injector = LifecycleInjector
            .builder()
            .usingBasePackages(PACKAGES)
            .withBootstrapModule
            (
                new BootstrapModule()
                {
                    @Override
                    public void configure(BootstrapBinder binder)
                    {
                        binder.bind(LifecycleListener.class).toInstance(new FilteredLifecycleListener(listener, PACKAGES));
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

    @Test
    public void     testSimpleSingleton() throws Exception
    {
        Injector            injector = LifecycleInjector.builder().usingBasePackages(PACKAGES).createInjector();
        LifecycleManager    manager = injector.getInstance(LifecycleManager.class);
        manager.start();

        SimpleSingleton     instance = injector.getInstance(SimpleSingleton.class);

        Assert.assertEquals(instance.startCount.get(), 1);
        Assert.assertEquals(instance.finishCount.get(), 0);

        manager.close();

        Assert.assertEquals(instance.startCount.get(), 1);
        Assert.assertEquals(instance.finishCount.get(), 1);
    }

    @Test
    public void     testInjectedIntoAnother() throws Exception
    {
        Injector            injector = LifecycleInjector.builder().usingBasePackages(PACKAGES).createInjector();
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
