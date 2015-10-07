package com.netflix.governator.auto.conditions;

import com.google.inject.Singleton;
import com.netflix.governator.auto.Condition;
import com.netflix.governator.auto.annotations.ConditionalOnSystem;

@Singleton
@Deprecated
/**
 * @deprecated Moved to Karyon3
 */
public class OnSystemCondition implements Condition<ConditionalOnSystem> {
    @Override
    public boolean check(ConditionalOnSystem condition) {
        String value = System.getProperty(condition.name());
        if (value == null || condition.value() == null) {
            return false;
        }
        return condition.value().equals(value);
    }
    
    @Override
    public String toString() {
        return "OnSystemCondition[]";
    }
}
