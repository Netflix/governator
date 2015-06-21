package com.netflix.governator.auto.conditions;

import com.netflix.governator.auto.Condition;
import com.netflix.governator.auto.annotations.ConditionalOnMissingClass;

public class OnMissingClassCondition implements Condition<ConditionalOnMissingClass> {
    @Override
    public boolean check(ConditionalOnMissingClass condition) {
        for (String name : condition.value()) {
            try {
                Class.forName(name);
                return false;
            } catch (ClassNotFoundException e) {
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "OnMissingClassCondition[]";
    }

}
