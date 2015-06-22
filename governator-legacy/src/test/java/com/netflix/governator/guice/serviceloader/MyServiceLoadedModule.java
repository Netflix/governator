package com.netflix.governator.guice.serviceloader;

import com.google.inject.AbstractModule;
import com.google.inject.name.Names;

public class MyServiceLoadedModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(String.class).annotatedWith(Names.named("MyServiceLoadedModule")).toInstance("loaded");
    }
}
