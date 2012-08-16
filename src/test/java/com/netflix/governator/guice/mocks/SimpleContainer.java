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

package com.netflix.governator.guice.mocks;

import com.netflix.governator.annotations.AutoBindSingleton;
import org.testng.Assert;
import javax.inject.Inject;

@AutoBindSingleton
public class SimpleContainer
{
    public final SimpleSingleton       simpleObject;

    @Inject
    public SimpleContainer(SimpleSingleton simpleObject)
    {
        this.simpleObject = simpleObject;
        Assert.assertEquals(simpleObject.startCount.get(), 1);
        Assert.assertEquals(simpleObject.finishCount.get(), 0);
    }
}
