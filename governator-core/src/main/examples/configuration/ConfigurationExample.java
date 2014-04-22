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

package configuration;

import com.google.inject.Injector;
import com.netflix.governator.configuration.SystemConfigurationProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;

public class ConfigurationExample
{
    public static void main(String[] args) throws Exception
    {
        // in this example we'll use system properties
        // with a variable "prefix" to select the property
        System.setProperty("first.a-string", "first string");
        System.setProperty("first.an-int", "1");
        System.setProperty("first.a-double", "1.1");
        System.setProperty("second.a-string", "second string");
        System.setProperty("second.an-int", "2");
        System.setProperty("second.a-double", "2.2");

        // execute the example twice. The first time with the prefix
        // set to "first". The second time with it set to "second"

        SystemConfigurationProvider     provider = new SystemConfigurationProvider();
        provider.setVariable("prefix", "first");
        doWork(provider);

        provider.setVariable("prefix", "second");
        doWork(provider);

        /*
            Console will show:
                preConfig
                first string
                1
                1.1
                preConfig
                second string
                2
                2.2
         */
    }

    private static void doWork(final SystemConfigurationProvider provider) throws Exception
    {
        // Always get the Guice injector from Governator
        Injector    injector = LifecycleInjector
            .builder()
            .withBootstrapModule
            (
                new BootstrapModule()
                {
                    @Override
                    public void configure(BootstrapBinder binder)
                    {
                        // bind the configuration provider
                        binder.bindConfigurationProvider().toInstance(provider);
                    }
                }
            )
            .createInjector();

        ExampleObject       obj = injector.getInstance(ExampleObject.class);

        LifecycleManager    manager = injector.getInstance(LifecycleManager.class);

        // Always start the Lifecycle Manager
        manager.start();

        System.out.println(obj.getAString());
        System.out.println(obj.getAnInt());
        System.out.println(obj.getADouble());

        // your app would execute here

        // Always close the Lifecycle Manager at app end
        manager.close();
    }
}
