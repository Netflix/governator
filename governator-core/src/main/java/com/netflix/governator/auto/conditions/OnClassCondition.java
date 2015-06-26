package com.netflix.governator.auto.conditions;

import com.google.inject.Singleton;
import com.netflix.governator.auto.Condition;
import com.netflix.governator.auto.annotations.ConditionalOnClass;

@Singleton
public class OnClassCondition implements Condition<ConditionalOnClass> {
    @Override
    public boolean check(ConditionalOnClass condition) {
        for (String name : condition.value()) {
            try {
                Class.forName(name, false, ClassLoader.getSystemClassLoader());
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "OnClassCondition[]";
    }
}
