package com.netflix.governator.internal;

import static com.netflix.governator.internal.AbstractLifecycleFeature.TypeVisitor.ElementType.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import com.netflix.governator.LifecycleAction;
import com.netflix.governator.LifecycleFeature;

/**
 * Base utility class for field and method based lifecycle features. This class
 * deals with identifying all the fields and methods for the type and its
 * superclasses and interfaces and delegating the actual processing using the
 * template methods getFieldAction and getMethodAction.
 * 
 * @author elandau
 */
abstract class AbstractLifecycleFeature implements LifecycleFeature {
    interface TypeVisitor {
        enum ElementType {
            FIELD, METHOD, INTERFACE, SUPERCLASS
        }

        boolean accept(ElementType elementType);

        List<LifecycleAction> getFieldActions(Class<?> type, Field field);

        List<LifecycleAction> getMethodActions(Class<?> type, Method method);

    }

    private void visitFieldsAndMethods(TypeVisitor typeVisitor, Class<?> type, List<LifecycleAction> actions) {
        if (type == null) {
            return;
        }

        if (typeVisitor.accept(FIELD)) {
            for (final Field field : type.getDeclaredFields()) {
                actions.addAll(typeVisitor.getFieldActions(type, field));
            }
        }

        if (typeVisitor.accept(METHOD)) {
            for (final Method method : type.getDeclaredMethods()) {
                actions.addAll(typeVisitor.getMethodActions(type, method));
            }
        }

        if (typeVisitor.accept(SUPERCLASS)) {
            visitFieldsAndMethods(typeVisitor, type.getSuperclass(), actions);
        }

        if (typeVisitor.accept(INTERFACE)) {
            for (Class<?> iface : type.getInterfaces()) {
                visitFieldsAndMethods(typeVisitor, iface, actions);
            }
        }
    }

    protected abstract TypeVisitor newTypeVisitor();

    @Override
    public List<LifecycleAction> getActionsForType(Class<?> type) {
        List<LifecycleAction> actions = new ArrayList<>();
        visitFieldsAndMethods(newTypeVisitor(), type, actions);
        return actions;
    }
}
