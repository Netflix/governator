package com.netflix.governator.configuration;

import com.google.common.base.Supplier;

public abstract class Property<T> {
    public static <T> Property<T> from(final T value) {
        return new Property<T>() {
            @Override
            public T get() {
                return value;
            }
        };
    }
    
    public static <T> Property<T> from(final Supplier<T> value) {
        return new Property<T>() {
            @Override
            public T get() {
                return value.get();
            }
        };
    }
    
    public static <T> Supplier<T> from(final Property<T> value) {
        return new Supplier<T>() {
            @Override
            public T get() {
                return value.get();
            }
        };
    }
    
    public abstract T get();
}