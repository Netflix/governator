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

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.governator.configuration.CompositeConfigurationProvider;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.configuration.PropertiesConfigurationProvider;
import com.netflix.governator.guice.AbstractBootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.mocks.ObjectWithConfig;
import com.netflix.governator.lifecycle.mocks.ObjectWithConfigVariable;
import com.netflix.governator.lifecycle.mocks.ObjectWithIgnoreTypeMismatchConfig;
import com.netflix.governator.lifecycle.mocks.PreConfigurationChange;
import com.netflix.governator.lifecycle.mocks.SubclassedObjectWithConfig;

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
        
        final CompositeConfigurationProvider provider = new CompositeConfigurationProvider(new PropertiesConfigurationProvider(properties));

        LifecycleInjector injector = LifecycleInjector.builder()
            .withBootstrapModule(new AbstractBootstrapModule() {
                @Override
                protected void configure() {
                    this.bindConfigurationProvider().toInstance(provider);
                    binder().bind(CompositeConfigurationProvider.class).toInstance(provider);
                }
            })
            .build();
        
        LifecycleManager            manager = injector.getLifecycleManager();
        PreConfigurationChange      test = injector.getInjector().getInstance(PreConfigurationChange.class);
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

        final CompositeConfigurationProvider provider = new CompositeConfigurationProvider(new PropertiesConfigurationProvider(properties));

        LifecycleInjector injector = LifecycleInjector.builder()
                .withBootstrapModule(new AbstractBootstrapModule() {
                    @Override
                    protected void configure() {
                        this.bindConfigurationProvider().toInstance(provider);
                        binder().bind(CompositeConfigurationProvider.class).toInstance(provider);
                    }
                })
                .build();
        
        LifecycleManager            manager = injector.getLifecycleManager();
        manager.start();
        
        SubclassedObjectWithConfig  obj = injector.getInjector().getInstance(SubclassedObjectWithConfig.class);

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

        final CompositeConfigurationProvider provider = new CompositeConfigurationProvider(new PropertiesConfigurationProvider(properties));

        LifecycleInjector injector = LifecycleInjector.builder()
                .withBootstrapModule(new AbstractBootstrapModule() {
                    @Override
                    protected void configure() {
                        this.bindConfigurationProvider().toInstance(provider);
                        binder().bind(CompositeConfigurationProvider.class).toInstance(provider);
                    }
                })
                .build();
        
        LifecycleManager            manager = injector.getLifecycleManager();
        manager.start();

        ObjectWithConfig    obj = injector.getInjector().getInstance(ObjectWithConfig.class);

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

        final CompositeConfigurationProvider provider = new CompositeConfigurationProvider(new PropertiesConfigurationProvider(properties));

        LifecycleInjector injector = LifecycleInjector.builder()
                .withBootstrapModule(new AbstractBootstrapModule() {
                    @Override
                    protected void configure() {
                        this.bindConfigurationProvider().toInstance(provider);
                        binder().bind(CompositeConfigurationProvider.class).toInstance(provider);
                    }
                })
                .build();
        
        LifecycleManager            manager = injector.getLifecycleManager();
        manager.start();

        ObjectWithConfigVariable    obj = new ObjectWithConfigVariable("test");
        injector.getInjector().injectMembers(obj);

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

    private void testTypeMismatch(final ConfigurationProvider provider) throws Exception {
        
        
        LifecycleInjector injector = LifecycleInjector.builder()
                .withBootstrapModule(new AbstractBootstrapModule() {
                    @Override
                    protected void configure() {
                        this.bindConfigurationProvider().toInstance(provider);
                    }
                })
                .build();
        
        
        injector.getLifecycleManager().start();

        ObjectWithIgnoreTypeMismatchConfig obj = injector.getInjector().getInstance(ObjectWithIgnoreTypeMismatchConfig.class);
        ObjectWithIgnoreTypeMismatchConfig nonGovernatedSample = injector.getInjector().getInstance(ObjectWithIgnoreTypeMismatchConfig.class);
        nonGovernatedSample.aDate = new Date(obj.aDate.getTime());

        Assert.assertEquals(obj.aBool, nonGovernatedSample.aBool);
        Assert.assertEquals(obj.anInt, nonGovernatedSample.anInt);
        Assert.assertEquals(obj.aLong, nonGovernatedSample.aLong);
        Assert.assertEquals(obj.aDouble, nonGovernatedSample.aDouble);
        Assert.assertEquals(obj.aDate, nonGovernatedSample.aDate);
    }
}
