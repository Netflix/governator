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

import com.netflix.governator.configuration.PropertiesConfigurationProvider;
import com.netflix.governator.lifecycle.mocks.ObjectWithConfig;
import com.netflix.governator.lifecycle.mocks.PreConfigurationChange;
import com.netflix.governator.lifecycle.mocks.SubclassedObjectWithConfig;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Properties;

public class TestConfiguration
{
    @Test
    public void     testPreConfiguration() throws Exception
    {
        Properties  properties = new Properties();
        properties.setProperty("pre-config-test", "not-default");

        LifecycleManagerArguments   arguments = new LifecycleManagerArguments();
        arguments.getConfigurationProvider().add(new PropertiesConfigurationProvider(properties));

        LifecycleManager            manager = new LifecycleManager(arguments);
        PreConfigurationChange      test = new PreConfigurationChange(arguments.getConfigurationProvider());
        manager.add(test);

        manager.start();

        // assertions in PreConfigurationChange
    }

    @Test
    public void     testConfigSubclass() throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty("test.b", "true");
        properties.setProperty("test.i", "100");
        properties.setProperty("test.l", "200");
        properties.setProperty("test.d", "300.4");
        properties.setProperty("test.s", "a is a");
        properties.setProperty("test.main", "2468");

        LifecycleManagerArguments   arguments = new LifecycleManagerArguments();
        arguments.getConfigurationProvider().add(new PropertiesConfigurationProvider(properties));

        LifecycleManager    manager = new LifecycleManager(arguments);

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
        Properties properties = new Properties();
        properties.setProperty("test.b", "true");
        properties.setProperty("test.i", "100");
        properties.setProperty("test.l", "200");
        properties.setProperty("test.d", "300.4");
        properties.setProperty("test.s", "a is a");
        properties.setProperty("test.dt", "1964-10-06");

        LifecycleManagerArguments   arguments = new LifecycleManagerArguments();
        arguments.getConfigurationProvider().add(new PropertiesConfigurationProvider(properties));

        LifecycleManager            manager = new LifecycleManager(arguments);

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
