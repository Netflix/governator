package com.netflix.governator.guice.modules;

import com.google.inject.AbstractModule;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.annotations.binding.Color;

@AutoBindSingleton
public class AutoBindModule2 extends AbstractModule
{
    @Override
    protected void configure()
    {
        binder().bind(String.class).annotatedWith(Color.class).toInstance("blue");
    }
}
