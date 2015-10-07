package com.netflix.governator.auto.conditions;

import javax.inject.Singleton;

import com.netflix.governator.auto.Condition;

@Singleton
@Deprecated
/**
 * @deprecated Moved to Karyon3
 */
public class OnTestNGCondition implements Condition<OnTestNGCondition>{
    @Override
    public boolean check(OnTestNGCondition param) {
        String cmd = System.getProperty("sun.java.command");
        if (cmd == null) {
            return false;
        }
        
        return cmd.startsWith("org.testng.remote.RemoteTestNG");
    }

    @Override
    public String toString() {
        return "OnTestNGCondition[]";
    }
}
