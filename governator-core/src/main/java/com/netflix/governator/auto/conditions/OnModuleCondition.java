package com.netflix.governator.auto.conditions;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.netflix.governator.auto.AutoContext;
import com.netflix.governator.auto.Condition;
import com.netflix.governator.auto.annotations.ConditionalOnMissingModule;

@Singleton
public class OnModuleCondition implements Condition<ConditionalOnMissingModule>{
    private final AutoContext context;
    
    @Inject
    public OnModuleCondition(AutoContext context) {
        this.context = context;
    }
    
    @Override
    public boolean check(ConditionalOnMissingModule param) {
        for (String module : param.value()) {
            if (!context.hasModule(module)) {
                return true;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "OnModuleCondition[]";
    }
}
