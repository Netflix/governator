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

import com.google.inject.Binder;
import com.netflix.governator.annotations.AutoBind;
import com.netflix.governator.guice.AutoBindProvider;
import java.util.concurrent.atomic.AtomicInteger;

public class ExampleAutoBindProvider implements AutoBindProvider<AutoBind>
{
    private final AtomicInteger     counter = new AtomicInteger();

    @Override
    public void configure(Binder binder, AutoBind annotation)
    {
        // this method will get called for each field/argument that is annotated
        // with @AutoBind. NOTE: the fields/methods/constructors must also
        // be annotated with @Inject

        String      value = annotation.value() + " - " + counter.incrementAndGet();
        binder.bind(String.class).annotatedWith(annotation).toInstance(value);
    }
}
