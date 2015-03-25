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

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.netflix.config.ConfigurationManager;
import com.netflix.governator.configuration.ArchaiusConfigurationProvider;
import com.netflix.governator.configuration.ConfigurationOwnershipPolicies;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.guice.AbstractBootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.mocks.ObjectWithDynamicConfig;
import com.netflix.governator.lifecycle.mocks.ObjectWithIgnoreTypeMismatchConfig;

public class TestConfiguration
{

    private static final String MAP_OF_MAPS_STRING = "{\"foo\": {\"bar\": \"baz\"}}";
    private static final Map<String, Map<String, String>> MAP_OF_MAPS_OBJ = new HashMap<String, Map<String, String>>();
    static {
        MAP_OF_MAPS_OBJ.put("foo", new HashMap<String, String>());
        MAP_OF_MAPS_OBJ.get("foo").put("bar", "baz");
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
        LifecycleInjector injector = LifecycleInjector.builder()
                .withBootstrapModule(new AbstractBootstrapModule() {
                    @Override
                    protected void configure() {
                        this.bindConfigurationProvider().toInstance(ArchaiusConfigurationProvider
                                .builder()
                                .withOwnershipPolicy(ConfigurationOwnershipPolicies.ownsAll())
                            .build());
                    }
                })
                .build();
        
        LifecycleManager            manager = injector.getLifecycleManager();
        manager.start();

        ObjectWithDynamicConfig    obj = injector.getInjector().getInstance(ObjectWithDynamicConfig.class);

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
