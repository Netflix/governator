package com.netflix.governator.auto.conditions;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.Key;
import com.netflix.governator.auto.AutoContext;
import com.netflix.governator.auto.Condition;
import com.netflix.governator.auto.annotations.ConditionalOnBinding;

@Singleton
@Deprecated
/**
 * @deprecated Moved to Karyon3
 */
public class OnBindingCondition implements Condition<ConditionalOnBinding> {
    private final AutoContext context;

    @Inject
    public OnBindingCondition(AutoContext context) {
        this.context = context;
    }
    
    @Override
    public boolean check(ConditionalOnBinding condition) {
        for (String name : condition.value()) {
            try {
                if (!context.hasBinding(Key.get(Class.forName(name, false, ClassLoader.getSystemClassLoader())))) {
                    return false;
                }
            } catch (ClassNotFoundException e) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public String toString() {
        return "OnBindingCondition[]";
    }
}
