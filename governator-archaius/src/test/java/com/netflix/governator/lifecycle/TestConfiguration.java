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

import com.netflix.config.ConfigurationManager;
import com.netflix.governator.configuration.*;
import com.netflix.governator.lifecycle.mocks.*;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.SimpleDateFormat;
import java.util.*;

public class TestConfiguration
{

    private static final String MAP_OF_MAPS_STRING = "{\"foo\": {\"bar\": \"baz\"}}";
    private static final Map<String, Map<String, String>> MAP_OF_MAPS_OBJ = new HashMap<String, Map<String, String>>();
    static {
        MAP_OF_MAPS_OBJ.put("foo", new HashMap<String, String>());
        MAP_OF_MAPS_OBJ.get("foo").put("bar", "baz");
    }

    @Test
    public void     testPreConfiguration() throws Exception
    {
        Properties  properties = new Properties();
        properties.setProperty("pre-config-test", "not-default");

        LifecycleManagerArguments   arguments = new LifecycleManagerArguments();
        CompositeConfigurationProvider compositeProvider = new CompositeConfigurationProvider();
        
        arguments.setConfigurationProvider(compositeProvider);
        compositeProvider.add(new PropertiesConfigurationProvider(properties));

        LifecycleManager            manager = new LifecycleManager(arguments);
        PreConfigurationChange      test = new PreConfigurationChange(compositeProvider);
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
        properties.setProperty("test.obj", "[1,2,3,4]");
        properties.setProperty("test.mapOfMaps", MAP_OF_MAPS_STRING);

        LifecycleManagerArguments   arguments = new LifecycleManagerArguments();
        arguments.setConfigurationProvider(new PropertiesConfigurationProvider(properties));

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
        Assert.assertEquals(obj.ints, Arrays.asList(1,2,3,4));
        Assert.assertEquals(obj.mapOfMaps, MAP_OF_MAPS_OBJ);
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
        properties.setProperty("test.obj", "[1,2,3,4]");
        properties.setProperty("test.mapOfMaps", MAP_OF_MAPS_STRING);

        LifecycleManagerArguments   arguments = new LifecycleManagerArguments();
        arguments.setConfigurationProvider(new PropertiesConfigurationProvider(properties));

        LifecycleManager            manager = new LifecycleManager(arguments);

        ObjectWithConfig    obj = new ObjectWithConfig();
        manager.add(obj);
        manager.start();

        Assert.assertEquals(obj.aBool, true);
        Assert.assertEquals(obj.anInt, 100);
        Assert.assertEquals(obj.aLong, 200);
        Assert.assertEquals(obj.aDouble, 300.4);
        Assert.assertEquals(obj.aString, "a is a");
        Assert.assertEquals(obj.ints, Arrays.asList(1,2,3,4));
        Assert.assertEquals(obj.mapOfMaps, MAP_OF_MAPS_OBJ);
    }

    @Test
    public void     testConfigWithVariable() throws Exception 
    {
        Properties properties = new Properties();
        properties.setProperty("test.b", "true");
        properties.setProperty("test.i", "101");
        properties.setProperty("test.l", "201");
        properties.setProperty("test.d", "301.4");
        properties.setProperty("test.s", "b is b");
        properties.setProperty("test.dt", "1965-10-06");
        properties.setProperty("test.obj", "[1,2,3,4]");

        LifecycleManagerArguments   arguments = new LifecycleManagerArguments();
        arguments.setConfigurationProvider(new PropertiesConfigurationProvider(properties));

        LifecycleManager            manager = new LifecycleManager(arguments);

        ObjectWithConfigVariable obj = new ObjectWithConfigVariable("test");        
        manager.add(obj);
        manager.start();

        Assert.assertEquals(obj.aBool, true);
        Assert.assertEquals(obj.anInt, 101);
        Assert.assertEquals(obj.aLong, 201);
        Assert.assertEquals(obj.aDouble, 301.4);
        Assert.assertEquals(obj.aString, "b is b");
        Assert.assertEquals(obj.ints, Arrays.asList(1,2,3,4));
    }
    
    @Test
    public void     testConfigTypeMismatchWithPropProvider() throws Exception
    {
        Properties properties = new Properties();
        properties.setProperty("test.b", "20");
        properties.setProperty("test.i", "foo");
        properties.setProperty("test.l", "bar");
        properties.setProperty("test.d", "zar");
        properties.setProperty("test.s", "a is a");
        properties.setProperty("test.dt", "dar");

        testTypeMismatch(new PropertiesConfigurationProvider(properties));
    }

    @Test
    public void     testConfigTypeMismatchWithArchaius() throws Exception
    {
        Map<String, String> properties = new HashMap<String, String>();
        properties.put("test.b", "20");
        properties.put("test.i", "foo");
        properties.put("test.l", "bar");
        properties.put("test.d", "zar");
        properties.put("test.s", "a is a");
        properties.put("test.dt", "dar");

        //noinspection deprecation
        testTypeMismatch(new ArchaiusConfigurationProvider(properties));
    }

    @Test
    public void     testDynamicConfiguration() throws Exception
    {
        LifecycleManagerArguments   arguments = new LifecycleManagerArguments();
        arguments.setConfigurationProvider(ArchaiusConfigurationProvider
                .builder()
                    .withOwnershipPolicy(ConfigurationOwnershipPolicies.ownsAll())
                .build());

        LifecycleManager            manager = new LifecycleManager(arguments);

        ObjectWithDynamicConfig    obj = new ObjectWithDynamicConfig();
        manager.add(obj);
        manager.start();

        Assert.assertEquals(obj.aDynamicBool.get(), Boolean.TRUE);
        Assert.assertEquals(obj.anDynamicInt.get(), new Integer(1));
        Assert.assertEquals(obj.anDynamicInt2.get(), new Integer(1));
        Assert.assertEquals(obj.aDynamicLong.get(), new Long(2L));
        Assert.assertEquals(obj.aDynamicDouble.get(), 3.4);
        Assert.assertEquals(obj.aDynamicString.get(), "a is a");
        Assert.assertEquals(obj.aDynamicString2.get(), "a is a");
        Assert.assertEquals(obj.aDynamicDate.get(), null);
        Assert.assertEquals(obj.aDynamicObj.get(), Arrays.asList(5, 6, 7));
        Assert.assertEquals(obj.aDynamicMapOfMaps.get(), Collections.emptyMap());

        ConfigurationManager.getConfigInstance().setProperty("test.dynamic.b", "false");
        ConfigurationManager.getConfigInstance().setProperty("test.dynamic.i", "101");
        ConfigurationManager.getConfigInstance().setProperty("test.dynamic.l", "201");
        ConfigurationManager.getConfigInstance().setProperty("test.dynamic.d", "301.4");
        ConfigurationManager.getConfigInstance().setProperty("test.dynamic.s", "a is b");
        ConfigurationManager.getConfigInstance().setProperty("test.dynamic.dt", "1964-11-06");
        ConfigurationManager.getConfigInstance().setProperty("test.dynamic.obj", "[1,2,3,4]");
        ConfigurationManager.getConfigInstance().setProperty("test.dynamic.mapOfMaps", MAP_OF_MAPS_STRING);

        Assert.assertEquals(obj.aDynamicBool.get(), Boolean.FALSE);
        Assert.assertEquals(obj.anDynamicInt.get(), new Integer(101));
        Assert.assertEquals(obj.aDynamicLong.get(), new Long(201L));
        Assert.assertEquals(obj.aDynamicDouble.get(), 301.4);
        Assert.assertEquals(obj.aDynamicString.get(), "a is b");
        Assert.assertEquals(obj.aDynamicObj.get(), Arrays.asList(1, 2, 3, 4));
        Assert.assertEquals(obj.aDynamicMapOfMaps.get(), MAP_OF_MAPS_OBJ);

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        Assert.assertEquals(obj.aDynamicDate.get(), formatter.parse("1964-11-06"));
    }
    
    private void testTypeMismatch(ConfigurationProvider provider) throws Exception {
        LifecycleManagerArguments arguments = new LifecycleManagerArguments();
        arguments.setConfigurationProvider(provider);

        LifecycleManager manager = new LifecycleManager(arguments);

        ObjectWithIgnoreTypeMismatchConfig obj = new ObjectWithIgnoreTypeMismatchConfig();
        ObjectWithIgnoreTypeMismatchConfig nonGovernatedSample = new ObjectWithIgnoreTypeMismatchConfig();
        nonGovernatedSample.aDate = new Date(obj.aDate.getTime());
        manager.add(obj);
        manager.start();


        Assert.assertEquals(obj.aBool, nonGovernatedSample.aBool);
        Assert.assertEquals(obj.anInt, nonGovernatedSample.anInt);
        Assert.assertEquals(obj.aLong, nonGovernatedSample.aLong);
        Assert.assertEquals(obj.aDouble, nonGovernatedSample.aDouble);
        Assert.assertEquals(obj.aDate, nonGovernatedSample.aDate);
    }
}
