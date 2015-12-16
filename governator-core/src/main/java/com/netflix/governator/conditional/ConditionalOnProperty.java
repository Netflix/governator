package com.netflix.governator.conditional;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.netflix.governator.spi.PropertySource;

/**
 * Conditional that evaluates to true if the a property is set to a specific value
 */
public class ConditionalOnProperty implements Conditional {
    private final String value;
    private final String key;

    public ConditionalOnProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean matches(Injector injector) {
        Binding<PropertySource> properties = injector.getExistingBinding(Key.get(PropertySource.class));
        if (properties == null) {
            return false;
        }
        return value.equals(properties.getProvider().get().get(key, ""));
    }
    
    @Override
    public String toString() {
        return "ConditionalOnProperty[" + key + "=" + value + "]";
    }
}
