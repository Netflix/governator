package com.netflix.governator;

import com.google.inject.AbstractModule;

public class DefaultModule extends AbstractModule {
    @Override
    protected void configure() {
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
