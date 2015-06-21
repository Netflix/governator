package com.netflix.governator.auto.conditions;

import javax.inject.Singleton;

import com.netflix.governator.auto.Condition;
import com.netflix.governator.auto.annotations.ConditionalOnMacOS;

@Singleton
public class OnMacOSCondition implements Condition<ConditionalOnMacOS>{
    @Override
    public boolean check(ConditionalOnMacOS param) {
        return "Mac OS X".equals(System.getProperty("os.name"));
    }
    
    @Override
    public String toString() {
        return "OnMacOSCondition[]";
    }

}
