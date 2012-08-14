package com.netflix.governator.inject.guice;

import com.google.inject.Binder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import javax.inject.Provider;

public class ProviderBinderUtil
{
    public static void      bind(Binder binder, final Class<? extends Provider> clazz)
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
            );
    }
    private ProviderBinderUtil()
    {
    }
}
