package com.netflix.governator.package1;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import javax.inject.Singleton;

public class Foo  extends AbstractModule {
    @Provides
    @Singleton
    Foo getFoo() {
        return new Foo();
    }

    @Override
    protected void configure() {
    }
}
