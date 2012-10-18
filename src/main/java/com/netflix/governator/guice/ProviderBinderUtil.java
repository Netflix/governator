package com.netflix.governator.guice;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import javax.inject.Provider;
import com.google.inject.Scope;

class ProviderBinderUtil
{
    static void      bind(Binder binder, final Class<? extends Provider> clazz, Scope scope)
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
