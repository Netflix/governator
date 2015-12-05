package com.netflix.governator.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.netflix.governator.LifecycleAction;
import com.netflix.governator.LifecycleFeature;

/**
 * Base utility class for field and method based lifecycle features.  This class
 * deals with identifying all the fields and methods for the type and its superclasses
 * and interfaces and delegating the actual processing using the template methods
 * getFieldAction and getMethodAction.
 * 
 * @author elandau
 */
abstract class AbstractLifecycleFeature implements LifecycleFeature {

    private void visitFieldsAndMethods(Class<?> type, List<LifecycleAction> actions) {
        if (type == null) {
            return;
        }
        
        for (final Field field : type.getDeclaredFields()) {
            actions.addAll(getFieldActions(type, field));
        }
        
        for (final Method method : type.getDeclaredMethods()) {
            actions.addAll(getMethodActions(type, method));
        }
        
        visitFieldsAndMethods(type.getSuperclass(), actions);
        for (Class<?> iface : type.getInterfaces()) {
            visitFieldsAndMethods(iface, actions);
        }
    }

    protected List<LifecycleAction> getFieldActions(Class<?> type, Field field) {
        return Collections.emptyList();
    }
    
    protected List<LifecycleAction> getMethodActions(Class<?> type, Method method) {
        return Collections.emptyList();
    }
    
    @Override
    public List<LifecycleAction> getActionsForType(Class<?> type) {
        List<LifecycleAction> actions = new ArrayList<>();
        visitFieldsAndMethods(type, actions);
        return actions;
    }
}
