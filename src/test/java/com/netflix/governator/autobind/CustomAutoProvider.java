package com.netflix.governator.autobind;

import com.google.inject.Binder;
import com.google.inject.Singleton;
import com.netflix.governator.guice.AutoBindProvider;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

@Singleton
public class CustomAutoProvider implements AutoBindProvider<CustomAutoBind>
{
    @Override
    public void configureForConstructor(Binder binder, CustomAutoBind custom, Constructor constructor, int argumentIndex)
    {
        binder.bind(MockInjectable.class).annotatedWith(custom).toInstance(new MockInjectable(custom.str(), custom.value()));
    }

    @Override
    public void configureForMethod(Binder binder, CustomAutoBind autoBindAnnotation, Method method, int argumentIndex)
    {
    }

    @Override
    public void configureForField(Binder binder, CustomAutoBind autoBindAnnotation, Field field)
    {
    }
}
