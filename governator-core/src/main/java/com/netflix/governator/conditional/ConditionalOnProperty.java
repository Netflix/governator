package com.netflix.governator.conditional;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.netflix.governator.spi.PropertySource;

/**
 * Conditional that evaluates to true if the a property is set to a specific value
 */
public class ConditionalOnProperty implements Conditional<ConditionalOnProperty> {
    private String value;
    private String key;

    public ConditionalOnProperty(String key, String value) {
        this.key = key;
        this.value = value;
    }

    @Override
    public Class<? extends Matcher<ConditionalOnProperty>> getMatcherClass() {
        return ConditionalOnPropertyMatcher.class;
    }

    @Override
    public String toString() {
        return "ConditionalOnProperty[" + key + "=" + value + "]";
    }
    
    @Singleton
    public static class ConditionalOnPropertyMatcher implements Matcher<ConditionalOnProperty> {
        @Inject(optional=true)
        PropertySource properties;
        
        @Override
        public boolean match(ConditionalOnProperty condition) {
            if (properties == null) {
                return false;
            }
            return condition.value.equals(properties.get(condition.key, ""));
        }
    }
}
