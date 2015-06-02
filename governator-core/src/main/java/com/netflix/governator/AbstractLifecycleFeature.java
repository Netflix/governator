package com.netflix.governator;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Base utility class for field and method based lifecycle features.  This class
 * deals with identifying all the fields and methods for the type and its superclasses
 * and interfaces and delgating the actual processing using the template methods
 * getFieldAction and getMethodAction.
 * 
 * @author elandau
 */
public abstract class AbstractLifecycleFeature implements LifecycleFeature {

    public static final LifecycleAction NONE = null;
    
    private void visitFieldsAndMethods(Class<?> type, List<LifecycleAction> actions) {
        if (type == null) {
            return;
        }
        
        for (final Field field : type.getDeclaredFields()) {
            LifecycleAction action = getFieldAction(type, field);
            if (action != NONE) {
                actions.add(action);
            }
        }
        
        for (final Method method : type.getDeclaredMethods()) {
            LifecycleAction action = getMethodAction(type, method);
            if (action != NONE) {
                actions.add(action);
            }
        }
        
        visitFieldsAndMethods(type.getSuperclass(), actions);
        for (Class<?> iface : type.getInterfaces()) {
            visitFieldsAndMethods(iface, actions);
        }
        
    }

    protected LifecycleAction getFieldAction(Class<?> type, Field field) {
        return NONE;
    }
    
    protected LifecycleAction getMethodAction(Class<?> type, Method method) {
        return NONE;
    }
    
    @Override
    public List<LifecycleAction> getActionsForType(Class<?> type) {
        List<LifecycleAction> actions = new ArrayList<>();
        visitFieldsAndMethods(type, actions);
        return actions;
    }
}
