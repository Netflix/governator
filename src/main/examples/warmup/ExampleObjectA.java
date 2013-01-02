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

import com.google.inject.Inject;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.annotations.WarmUp;

@AutoBindSingleton
public class ExampleObjectA
{
    private final ExampleObjectB b;
    private final ExampleObjectC c;

    @Inject
    public ExampleObjectA(ExampleObjectB b, ExampleObjectC c)
    {
        this.b = b;
        this.c = c;
        System.out.println("b.isWarm() " + b.isWarm());
        System.out.println("c.isWarm() " + c.isWarm());
    }

    @WarmUp
    public void     warmUp() throws InterruptedException
    {
        System.out.println("b.isWarm() " + b.isWarm());
        System.out.println("c.isWarm() " + c.isWarm());

        System.out.println("ExampleObjectA warm up start");
        Thread.sleep(1000);
        System.out.println("ExampleObjectA warm up end");
    }
}
