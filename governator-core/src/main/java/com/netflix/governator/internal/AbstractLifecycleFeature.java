package com.netflix.governator.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

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
abstract class AbstractLifecycleFeature<C> implements LifecycleFeature {
	enum IntrospectionFlag {
		FIELD, METHOD, INTERFACE, SUPERCLASS
	}

	private Set<IntrospectionFlag> flags;
	
	private AbstractLifecycleFeature(Set<IntrospectionFlag> flags) {
		this.flags = flags;
	}
	
	protected AbstractLifecycleFeature(IntrospectionFlag... flags) {
		this.flags = EnumSet.copyOf(Arrays.asList(flags));
	}
	
	protected AbstractLifecycleFeature() {
		this(EnumSet.allOf(IntrospectionFlag.class));
	}
	
    private void visitFieldsAndMethods(C typeContext, Class<?> type, List<LifecycleAction> actions) {
        if (type == null) {
            return;
        }
        
        if (flags.contains(IntrospectionFlag.FIELD)) {
	        for (final Field field : type.getDeclaredFields()) {
	            actions.addAll(getFieldActions(typeContext, type, field));
	        }
        }
        
        if (flags.contains(IntrospectionFlag.METHOD)) {
	        for (final Method method : type.getDeclaredMethods()) {
	            actions.addAll(getMethodActions(typeContext, type, method));
	        }
        }
        
        if (flags.contains(IntrospectionFlag.SUPERCLASS)) {
        	visitFieldsAndMethods(typeContext, type.getSuperclass(), actions);
        }
        
        if (flags.contains(IntrospectionFlag.INTERFACE)) {
	        for (Class<?> iface : type.getInterfaces()) {
	            visitFieldsAndMethods(typeContext, iface, actions);
	        }
        }
    }
    
    protected abstract C newTypeContext();

    protected List<LifecycleAction> getFieldActions(C typeContext, Class<?> type, Field field) {
        return Collections.emptyList();
    }
    
    protected List<LifecycleAction> getMethodActions(C typeContext, Class<?> type, Method method) {
        return Collections.emptyList();
    }
    
    @Override
    public List<LifecycleAction> getActionsForType(Class<?> type) {
        List<LifecycleAction> actions = new ArrayList<>();
        visitFieldsAndMethods(newTypeContext(), type, actions);
        return actions;
    }
}
