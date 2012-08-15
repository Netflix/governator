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

import com.netflix.governator.lifecycle.mocks.ObjectWithConfig;
import com.netflix.governator.lifecycle.mocks.SubclassedObjectWithConfig;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestConfiguration
{
    @Test
    public void     testConfigSubclass() throws Exception
    {
        LifecycleManager    manager = new LifecycleManager();

        System.setProperty("test.b", "true");
        System.setProperty("test.i", "100");
        System.setProperty("test.l", "200");
        System.setProperty("test.d", "300.4");
        System.setProperty("test.s", "a is a");
        System.setProperty("test.main", "2468");

        SubclassedObjectWithConfig  obj = new SubclassedObjectWithConfig();
        manager.add(obj);
        manager.start();

        Assert.assertEquals(obj.aBool, true);
        Assert.assertEquals(obj.anInt, 100);
        Assert.assertEquals(obj.aLong, 200);
        Assert.assertEquals(obj.aDouble, 300.4);
        Assert.assertEquals(obj.aString, "a is a");
        Assert.assertEquals(obj.mainInt, 2468);
    }

    @Test
    public void     testConfig() throws Exception
    {
        LifecycleManager    manager = new LifecycleManager();

        System.setProperty("test.b", "true");
        System.setProperty("test.i", "100");
        System.setProperty("test.l", "200");
        System.setProperty("test.d", "300.4");
        System.setProperty("test.s", "a is a");
        System.setProperty("test.dt", "1964-10-06");

        ObjectWithConfig    obj = new ObjectWithConfig();
        manager.add(obj);
        manager.start();

        Assert.assertEquals(obj.aBool, true);
        Assert.assertEquals(obj.anInt, 100);
        Assert.assertEquals(obj.aLong, 200);
        Assert.assertEquals(obj.aDouble, 300.4);
        Assert.assertEquals(obj.aString, "a is a");
    }
}
