/*
 * Copyright 2013 Netflix, Inc.
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

import com.google.inject.Injector;
import com.netflix.governator.LifecycleInjectorBuilderProvider;
import com.netflix.governator.configuration.PropertiesConfigurationProvider;
import com.netflix.governator.guice.mocks.ObjectWithConfig;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Properties;

public class TestInjectedConfiguration extends LifecycleInjectorBuilderProvider
{
    @Test(dataProvider = "builders")
    public void     testConfigurationProvider(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        final Properties    properties = new Properties();
        properties.setProperty("a", "1");
        properties.setProperty("b", "2");
        properties.setProperty("c", "3");

        Injector            injector = lifecycleInjectorBuilder
            .withBootstrapModule
            (
                new BootstrapModule()
                {
                    @Override
                    public void configure(BootstrapBinder binder)
                    {
                        binder.bindConfigurationProvider().toInstance(new PropertiesConfigurationProvider(properties));
                    }
                }
            )
            .createInjector();

        ObjectWithConfig        obj = injector.getInstance(ObjectWithConfig.class);
        Assert.assertEquals(obj.a, 1);
        Assert.assertEquals(obj.b, 2);
        Assert.assertEquals(obj.c, 3);
    }
}
