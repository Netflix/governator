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

package custom_autobind;

import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.netflix.governator.guice.AutoBindProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;

public class CustomAutoBindExample
{
    public static void main(String[] args) throws Exception
    {
        System.setProperty("prop-a", "ExampleObjectA should see this");

        // Always get the Guice injector from Governator
        Injector injector = LifecycleInjector
            .builder()
            .usingBasePackages("custom_autobind")
            .withBootstrapModule
                (
                    new BootstrapModule()
                    {
                        @Override
                        public void configure(BootstrapBinder binder)
                        {
                            // bind an AutoBindProvider for @ExampleAutoBind annotated fields/arguments
                            TypeLiteral<AutoBindProvider<ExampleAutoBind>> typeLiteral = new TypeLiteral<AutoBindProvider<ExampleAutoBind>>(){};
                            binder.bind(typeLiteral).to(ExampleAutoBindProvider.class).asEagerSingleton();
                        }
                    }
                )
            .createInjector();

        LifecycleManager manager = injector.getInstance(LifecycleManager.class);

        // Always start the Lifecycle Manager
        manager.start();

        System.out.println(injector.getInstance(ExampleObjectA.class).getValue());
        System.out.println(injector.getInstance(ExampleObjectB.class).getValue());
        System.out.println(injector.getInstance(ExampleObjectC.class).getValue());

        /*
            Console will output:
                ExampleObjectA should see this
                b
                c
         */

        // your app would execute here

        // Always close the Lifecycle Manager at app end
        manager.close();
    }
}
