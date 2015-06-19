package com.netflix.governator.auto.conditions;

import javax.inject.Singleton;

import com.netflix.governator.auto.Condition;

@Singleton
public class OnMacOSCondition implements Condition<OnMacOSCondition>{
    @Override
    public boolean check(OnMacOSCondition param) {
        return "Mac OS X".equals(System.getProperty("os.name"));
    }
}
