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

import com.google.inject.Binder;
import com.netflix.governator.annotations.AutoBind;
import java.lang.annotation.Annotation;

/**
 * Used to perform the binding for a given {@link AutoBind} annotation
 */
public interface AutoBindProvider<T extends Annotation>
{
    /**
     * Called for auto binding of constructor arguments
     *
     * @param binder the Guice binder
     * @param autoBindAnnotation the @AutoBind or custom annotation
     */
    public void     configure(Binder binder, T autoBindAnnotation);
}
