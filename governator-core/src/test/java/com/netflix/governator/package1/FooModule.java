package com.netflix.governator.package1;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.governator.annotations.AutoBindSingleton;

import javax.inject.Singleton;

@AutoBindSingleton
public class FooModule extends AbstractModule {
    @Provides
    @Singleton
    Foo getFoo() {
        return new Foo();
    }

    @Override
    protected void configure() {
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return getClass().equals(obj.getClass());
    }
}
