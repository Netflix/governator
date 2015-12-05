package com.netflix.governator.internal;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public final class ModulesEx {
    private static final Module EMPTY_MODULE = new AbstractModule() {
            @Override
            protected void configure() {
            }
        };
        
    public static Module emptyModule() {
        return EMPTY_MODULE;
    }
    
    public static Module fromEagerSingleton(final Class<?> type) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind(type).asEagerSingleton();
            }
        };
    }

    public static <T> Module fromInstance(final T object) {
        return new AbstractModule() {
            @Override
            protected void configure() {
                bind((Class<T>)object.getClass()).toInstance(object);
                this.requestInjection(object);
            }
        };
    }
}
