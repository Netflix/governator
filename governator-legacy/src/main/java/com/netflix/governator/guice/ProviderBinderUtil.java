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
import com.google.inject.Inject;
import com.google.inject.Injector;
import javax.inject.Provider;
import com.google.inject.Scope;

class ProviderBinderUtil
{
    static void      bind(Binder binder, Class<? extends Provider> clazz, Scope scope)
    {
        Class<?> providedType;
        try
        {
            providedType = clazz.getMethod("get").getReturnType();
        }
        catch ( NoSuchMethodException e )
        {
            throw new RuntimeException(e);
        }

        //noinspection unchecked
        binder.bind(providedType)
            .toProvider
            (
                new MyProvider(clazz)
            )
            .in(scope);
    }

    private ProviderBinderUtil()
    {
    }

    private static class MyProvider implements com.google.inject.Provider
    {
        private final Class<? extends Provider> clazz;

        @Inject
        private Injector injector;

        @Inject
        public MyProvider(Class<? extends Provider> clazz)
        {
            this.clazz = clazz;
        }

        @Override
        public Object get()
        {
            Provider provider = injector.getInstance(clazz);
            return provider.get();
        }
    }
}
