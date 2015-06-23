package com.netflix.governator.auto.conditions;

import com.google.inject.Singleton;
import com.netflix.governator.auto.Condition;
import com.netflix.governator.auto.annotations.ConditionalOnProperty;

@Singleton
public class OnEnvironmentCondition implements Condition<ConditionalOnProperty> {
    @Override
    public boolean check(ConditionalOnProperty condition) {
        String value = System.getenv(condition.name());
        if (value == null || condition.value() == null) {
            return false;
        }
        return condition.value().equals(value);
    }
    
    @Override
    public String toString() {
        return "OnEnvironmentCondition[]";
    }
}
