package com.netflix.governator.auto.conditions;

import com.netflix.governator.auto.Condition;
import com.netflix.governator.auto.annotations.ConditionalOnMissingClass;

@Deprecated
/**
 * @deprecated Moved to Karyon3
 */
public class OnMissingClassCondition implements Condition<ConditionalOnMissingClass> {
    @Override
    public boolean check(ConditionalOnMissingClass condition) {
        for (String name : condition.value()) {
            try {
                Class.forName(name, false, ClassLoader.getSystemClassLoader());
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
