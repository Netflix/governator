package com.netflix.governator.configuration;

public abstract class Property<T> {
    public static <T> Property<T> from(final T value) {
        return new Property<T>() {
            @Override
            public T get() {
                return value;
            }
        };
    }
    public abstract T get();
}
