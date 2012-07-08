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

package com.netflix.governator.inject.guice;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.netflix.governator.inject.guice.mocks.SimpleContainer;
import com.netflix.governator.inject.guice.mocks.SimpleSingleton;
import com.netflix.governator.lifecycle.LifecycleManager;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestGovernatorGuice
{
    @Test
    public void     testSimpleSingleton() throws Exception
    {
        LifecycleManager        manager = new LifecycleManager();
        Injector injector = Guice.createInjector
        (
            new LifecycleModule(manager),
            new GuiceAutoBindModule()
        );

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
        LifecycleManager        manager = new LifecycleManager();
        Injector injector = Guice.createInjector
        (
            new LifecycleModule(manager),
            new GuiceAutoBindModule()
        );

        manager.start();

        SimpleContainer     instance = injector.getInstance(SimpleContainer.class);

        Assert.assertEquals(instance.simpleObject.startCount.get(), 1);
        Assert.assertEquals(instance.simpleObject.finishCount.get(), 0);

        manager.close();

        Assert.assertEquals(instance.simpleObject.startCount.get(), 1);
        Assert.assertEquals(instance.simpleObject.finishCount.get(), 1);
    }
}
