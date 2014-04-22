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

package warmup;

import com.google.inject.Injector;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;

public class WarmUpExample
{
    public static void main(String[] args) throws Exception
    {
        // Always get the Guice injector from Governator
        Injector injector = LifecycleInjector.builder().createInjector();
        injector.getInstance(ExampleObjectA.class);

        LifecycleManager manager = injector.getInstance(LifecycleManager.class);

        // Always start the Lifecycle Manager
        manager.start();

        // by calling start() warm up begins. The console will show
        // something like this (the order may be slightly different):
        /*
            b.isWarm() false
            c.isWarm() false
            ExampleObjectB warm up start
            ExampleObjectC warm up start
            ExampleObjectB warm up end
            ExampleObjectC warm up end
            b.isWarm() true
            c.isWarm() true
            ExampleObjectA warm up start
            ExampleObjectA warm up end
         */

        // your app would execute here

        // Always close the Lifecycle Manager at app end
        manager.close();
    }
}
