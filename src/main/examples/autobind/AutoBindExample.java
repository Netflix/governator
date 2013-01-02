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

package autobind;

import com.google.common.collect.Lists;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.netflix.governator.annotations.AutoBind;
import com.netflix.governator.guice.AutoBindProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;
import java.util.List;

public class AutoBindExample
{
    public static void main(String[] args) throws Exception
    {
        // This Governator feature tells the CLASSPATH scanner
        // to ignore listed classes. So, even though ExampleService
        // is annotated with @AutoBindSingleton it will not
        // get created in this example
        List<Class<?>> ignore = Lists.newArrayList();
        ignore.add(ExampleService.class);

        // Always get the Guice injector from Governator
        Injector        injector = LifecycleInjector
            .builder()
            .usingBasePackages("autobind")
            .ignoringAutoBindClasses(ignore)    // tell Governator's CLASSPATH scanner to ignore listed classes
            .withBootstrapModule
            (
                new BootstrapModule()
                {
                    @Override
                    public void configure(BootstrapBinder binder)
                    {
                        // bind an AutoBindProvider for @AutoBind annotated fields/arguments
                        TypeLiteral<AutoBindProvider<AutoBind>>     typeLiteral = new TypeLiteral<AutoBindProvider<AutoBind>>(){};
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
                letter A - 1
                letter B - 2
                letter C - 3
         */

        // your app would execute here

        // Always close the Lifecycle Manager at app end
        manager.close();
    }
}
