package com.netflix.governator.guice.modules;

import com.google.inject.Binder;
import com.google.inject.Module;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.annotations.binding.Size;

@AutoBindSingleton
public class AutoBindModule1 implements Module
{
    @Override
    public void configure(Binder binder)
    {
        binder.bind(String.class).annotatedWith(Size.class).toInstance("large");
    }
}
