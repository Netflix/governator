package com.netflix.governator.auto.conditions;

import javax.inject.Inject;

import com.google.inject.Singleton;
import com.netflix.governator.auto.Condition;
import com.netflix.governator.auto.PropertySource;
import com.netflix.governator.auto.annotations.ConditionalOnProperty;

@Singleton
@Deprecated
/**
 * @deprecated Moved to Karyon3
 */
public class OnPropertyCondition implements Condition<ConditionalOnProperty> {

    private PropertySource config;

    @Inject
    public OnPropertyCondition(PropertySource config) {
        this.config = config;
    }
    
    @Override
    public boolean check(ConditionalOnProperty condition) {
        String value = config.get(condition.name());
        if (value == null || condition.value() == null) {
            return false;
        }
        return condition.value().equals(value);
    }
    
    @Override
    public String toString() {
        return "OnPropertyCondition[]";
    }
}
