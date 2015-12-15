package com.netflix.governator.conditional;

import com.google.inject.Inject;
import com.netflix.governator.spi.PropertySource;

/**
 * Conditional that evaluates to true if the a property is set to a specific value
 */
public class ConditionalOnProperty extends AbstractConditional {
    private final String value;
    private final String key;

    @Inject(optional=true)
    PropertySource properties;
    
    public ConditionalOnProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public boolean evaluate() {
        if (properties == null) {
            return false;
        }
        return value.equals(properties.get(key, ""));
    }
    
    @Override
    public String toString() {
        return "ConditionalOnProperty[" + key + "=" + value + "]";
    }
}
