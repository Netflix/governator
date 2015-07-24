package com.netflix.governator.auto.conditions;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.netflix.governator.auto.AutoContext;
import com.netflix.governator.auto.Condition;
import com.netflix.governator.auto.annotations.ConditionalOnModule;

@Singleton
public class OnModuleCondition implements Condition<ConditionalOnModule> {
    private final AutoContext context;
    
    @Inject
    public OnModuleCondition(AutoContext context) {
        this.context = context;
    }
    
    @Override
    public boolean check(ConditionalOnModule param) {
        for (Class<?> module : param.value()) {
            if (!context.hasModule(module.getName())) {
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "OnModuleCondition[]";
    }
}
