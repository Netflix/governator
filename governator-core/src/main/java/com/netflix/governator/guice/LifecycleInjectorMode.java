/*
 * Copyright 2014 Netflix, Inc.
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

public enum LifecycleInjectorMode
{
    /**
     * @deprecated using Guice child injectors has unwanted side effects. It also makes some patterns (e.g. injecting the Injector) difficult
     */
    REAL_CHILD_INJECTORS,

    /**
     * In this mode {@link LifecycleInjector} no longer uses Guice child injectors. Instead, bootstrap objects are copied into a new injector
     */
    SIMULATED_CHILD_INJECTORS
}
