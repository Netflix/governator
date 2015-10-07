package com.netflix.governator.auto.conditions;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.netflix.governator.auto.AutoContext;
import com.netflix.governator.auto.Condition;
import com.netflix.governator.auto.annotations.ConditionalOnProfile;

@Singleton
@Deprecated
/**
 * @deprecated Moved to Karyon3
 */
public class OnProfileCondition implements Condition<ConditionalOnProfile> {

    private AutoContext context;

    @Inject
    public OnProfileCondition(AutoContext context) {
        this.context = context;
    }
    
    @Override
    public boolean check(ConditionalOnProfile condition) {
        if (condition.matchAll()) {
            for (String profile : condition.value()) {
                if (!context.hasProfile(profile))
                    return false;
            }
            return true;
        }
        else {
            for (String profile : condition.value()) {
                if (!context.hasProfile(profile))
                    return true;
            }
            return false;
        }
    }
    
    @Override
    public String toString() {
        return "OnProfileCondition[]";
    }
}
