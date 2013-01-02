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

package lifecycle;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@Singleton
public class ExampleService
{
    @Inject
    public ExampleService(ExampleResource resource)
    {
        System.out.println("ExampleService construction");
    }

    @PostConstruct
    public void setup()
    {
        System.out.println("ExampleService setup");
    }

    @PreDestroy
    public void tearDown()
    {
        System.out.println("ExampleService tearDown");
    }
}
