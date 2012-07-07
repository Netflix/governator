/*
 * Copyright 2012 Netflix, Inc.
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

package com.netflix.governator.inject.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import com.netflix.governator.inject.ClasspathScanner;
import java.util.Set;

public class GuiceAutoBindModule extends AbstractModule
{
    private final AutoBindModes mode;

    public GuiceAutoBindModule(AutoBindModes mode)
    {
        this.mode = mode;
    }

    @Override
    protected void configure()
    {
        ClasspathScanner        scanner = new ClasspathScanner(Singleton.class);
        Set<Class<?>>           classes = (mode == AutoBindModes.ALL) ? scanner.getAll() : scanner.getAutoBindSingletons();
        for ( Class<?> clazz : classes )
        {
            binder().bind(clazz).asEagerSingleton();
        }
    }
}
