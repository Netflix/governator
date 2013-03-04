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

package com.netflix.governator.lifecycle.mocks;

import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.LifecycleState;
import org.testng.Assert;

public class SimpleContainer extends SimpleObject
{
    public final SimpleObject simpleObject;

    public SimpleContainer(LifecycleManager manager, SimpleObject simpleObject)
    {
        Assert.assertEquals(manager.getState(simpleObject), LifecycleState.ACTIVE);
        this.simpleObject = simpleObject;
    }
}
