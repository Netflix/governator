package com.netflix.governator.auto.conditions;

import javax.inject.Singleton;

import com.netflix.governator.auto.Condition;

@Singleton
@Deprecated
/**
 * @deprecated Moved to Karyon3
 */
public class OnJUnitCondition implements Condition<OnJUnitCondition>{
    @Override
    public boolean check(OnJUnitCondition param) {
        String cmd = System.getProperty("sun.java.command");
        if (cmd == null) {
            return false;
        }
        
        return cmd.startsWith("org.eclipse.jdt.internal.junit.runner");
        // TODO: Add additional checks for other IDEs
    }
    
    @Override
    public String toString() {
        return "OnJUnitCondition[]";
    }

}
