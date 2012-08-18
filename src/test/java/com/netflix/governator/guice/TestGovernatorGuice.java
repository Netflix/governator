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

import com.google.common.collect.Lists;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.governator.guice.mocks.SimpleContainer;
import com.netflix.governator.guice.mocks.SimpleEagerSingleton;
import com.netflix.governator.guice.mocks.SimplePojo;
import com.netflix.governator.guice.mocks.SimplePojoAlt;
import com.netflix.governator.guice.mocks.SimpleProvider;
import com.netflix.governator.guice.mocks.SimpleProviderAlt;
import com.netflix.governator.guice.mocks.SimpleSingleton;
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
    public void     testAutoBindSingletonMode() throws Exception
    {
        final List<Object>  objects = Lists.newArrayList();
        LifecycleListener   listener = new LifecycleListener()
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
            .withLifecycleListener(new FilteredLifecycleListener(listener, PACKAGES))
            .createInjector();

        Assert.assertEquals(objects.size(), 1);
        SimpleEagerSingleton obj = injector.getInstance(SimpleEagerSingleton.class);
        Assert.assertSame(obj, objects.get(0));

        SimpleSingleton obj2 = injector.getInstance(SimpleSingleton.class);
        Assert.assertEquals(objects.size(), 2);
        Assert.assertSame(obj2, objects.get(1));
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
                    ProviderBinderUtil.bind(binder(), SimpleProvider.class, SingletonMode.EAGER);
                    ProviderBinderUtil.bind(binder(), SimpleProviderAlt.class, SingletonMode.EAGER);
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
