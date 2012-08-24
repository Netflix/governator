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

import com.netflix.governator.lifecycle.mocks.SimpleContainer;
import com.netflix.governator.lifecycle.mocks.SimpleObject;
import org.testng.Assert;
import org.testng.annotations.Test;
import javax.validation.constraints.Min;

@SuppressWarnings("UnusedDeclaration")
public class TestLifecycleManager
{
    @Test
    public void     testValidation() throws Exception
    {
        Object          goodObj = new Object()
        {
            @Min(1)
            private int     a = 10;
        };

        Object          badObj = new Object()
        {
            @Min(1)
            private int     a = 0;
        };

        LifecycleManager    manager = new LifecycleManager();
        manager.add(goodObj);
        manager.start();

        manager = new LifecycleManager();
        manager.add(badObj);
        try
        {
            manager.start();
            Assert.fail();
        }
        catch ( ValidationException e )
        {
            // correct
        }
    }

    @Test
    public void     testSimple() throws Exception
    {
        LifecycleManager    manager = new LifecycleManager();
        manager.start();

        SimpleObject        simpleObject = new SimpleObject();

        Assert.assertEquals(manager.getState(simpleObject), LifecycleState.LATENT);
        Assert.assertEquals(simpleObject.startCount.get(), 0);
        Assert.assertEquals(simpleObject.finishCount.get(), 0);

        manager.add(simpleObject);
        Assert.assertEquals(manager.getState(simpleObject), LifecycleState.ACTIVE);
        Assert.assertEquals(simpleObject.startCount.get(), 1);
        Assert.assertEquals(simpleObject.finishCount.get(), 0);

        manager.close();
        Assert.assertEquals(manager.getState(simpleObject), LifecycleState.LATENT);
        Assert.assertEquals(simpleObject.startCount.get(), 1);
        Assert.assertEquals(simpleObject.finishCount.get(), 1);
    }

    @Test
    public void     testSimpleContainer() throws Exception
    {
        LifecycleManager    manager = new LifecycleManager();
        manager.start();

        SimpleObject        simpleObject = new SimpleObject();
        manager.add(simpleObject);

        SimpleContainer     simpleContainer = new SimpleContainer(manager, simpleObject);
        manager.add(simpleContainer);

        Assert.assertEquals(manager.getState(simpleObject), LifecycleState.ACTIVE);
        Assert.assertEquals(simpleObject.startCount.get(), 1);
        Assert.assertEquals(simpleObject.finishCount.get(), 0);
        Assert.assertEquals(manager.getState(simpleContainer), LifecycleState.ACTIVE);
        Assert.assertEquals(simpleContainer.startCount.get(), 1);
        Assert.assertEquals(simpleContainer.finishCount.get(), 0);

        manager.close();

        Assert.assertEquals(manager.getState(simpleObject), LifecycleState.LATENT);
        Assert.assertEquals(simpleObject.startCount.get(), 1);
        Assert.assertEquals(simpleObject.finishCount.get(), 1);
        Assert.assertEquals(manager.getState(simpleContainer), LifecycleState.LATENT);
        Assert.assertEquals(simpleContainer.startCount.get(), 1);
        Assert.assertEquals(simpleContainer.finishCount.get(), 1);
    }
}
