package com.netflix.governator.auto.conditions;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.netflix.governator.auto.AutoContext;
import com.netflix.governator.auto.Condition;
import com.netflix.governator.auto.annotations.ConditionalOnMissingModule;

@Singleton
@Deprecated
/**
 * @deprecated Moved to Karyon3
 */
public class OnMissingModuleCondition implements Condition<ConditionalOnMissingModule>{
    private final AutoContext context;
    
    @Inject
    public OnMissingModuleCondition(AutoContext context) {
        this.context = context;
    }
    
    @Override
    public boolean check(ConditionalOnMissingModule param) {
        for (String module : param.value()) {
            if (context.hasModule(module)) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "OnMissingModuleCondition[]";
    }

}
