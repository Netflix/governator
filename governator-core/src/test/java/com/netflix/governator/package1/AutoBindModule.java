package com.netflix.governator.package1;

import com.google.inject.AbstractModule;
import com.netflix.governator.annotations.AutoBindSingleton;

@AutoBindSingleton
public class AutoBindModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(String.class).toInstance("AutoBound");
    }
}
