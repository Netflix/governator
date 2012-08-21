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
                new com.google.inject.Provider()
                {
                    @Inject
                    private Injector injector;

                    @Override
                    public Object get()
                    {
                        Provider provider = injector.getInstance(clazz);
                        return provider.get();
                    }
                }
            )
            .in(scope);
    }
    private ProviderBinderUtil()
    {
    }
}
