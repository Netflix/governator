package com.netflix.governator;

import java.lang.reflect.Method;

import com.netflix.governator.spi.PropertySource;

public abstract class AbstractPropertySource implements PropertySource {
    @Override
    public <T> T get(String key, Class<T> type) {
        return get(key, type, null);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T get(String key, Class<T> type, T defaultValue) {
        String value = get(key);
        if (value == null) {
            return defaultValue;
        }
        Method method;
        try {
            method = type.getDeclaredMethod("valueOf", String.class);
        } catch (Exception e) {
            throw new RuntimeException("Unable to find method 'valueOf' of type '" + type.getName() + "'");
        }
        
        try {
            return (T) method.invoke(null, value);
        } catch (Exception e) {
            throw new RuntimeException("Unable to invoke method 'valueOf' of type '" + type.getName() + "'");
        }
    }

}
