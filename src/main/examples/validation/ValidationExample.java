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

package validation;

import com.google.inject.Injector;
import com.netflix.governator.configuration.SystemConfigurationProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.ValidationException;

public class ValidationExample
{
    public static void main(String[] args) throws Exception
    {
        // this example combines a number of Governator features.
        // It shows validation, AutoBindSingleton and @Configuration

        try
        {
            // There are no properties set which will cause
            // the fields of ExampleObject to stay at their defaults.
            // Their defaults violate their constraint annotations
            // thus an exception will be thrown
            doWork();
        }
        catch ( ValidationException e )
        {
            // correct
        }

        // set the annotation properties so that the constraints are not violated
        System.setProperty("value", "8");
        System.setProperty("str", "a string");
        doWork();
    }

    private static void        doWork() throws Exception
    {
        // Always get the Guice injector from Governator
        Injector    injector = LifecycleInjector
            .builder()
            .usingBasePackages("validation")
            .withBootstrapModule
                (
                    new BootstrapModule()
                    {
                        @Override
                        public void configure(BootstrapBinder binder)
                        {
                            binder.bindConfigurationProvider().to(SystemConfigurationProvider.class);
                        }
                    }
                )
            .createInjector();

        LifecycleManager    manager = injector.getInstance(LifecycleManager.class);

        // Always start the Lifecycle Manager
        manager.start();

        // your app would execute here

        // Always close the Lifecycle Manager at app end
        manager.close();
    }
}
