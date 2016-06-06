package com.netflix.governator.internal;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.google.common.base.Supplier;

/**
 * Utility class for class, field and method-based introspection.
 * 
 * @author elandau
 */
public class TypeInspector {
    /**
     * visitor interface for introspection processing
     */
    public interface TypeVisitor {

        boolean visit(Field field);

        boolean visit(Method method);

        boolean visit(Class<?> clazz);
    }
    
    public static <R, V extends Supplier<R>&TypeVisitor> R accept(Class<?> type, V visitor) {
        accept(type, (TypeVisitor)visitor);
        return visitor.get();
    }    

    public static void accept(Class<?> type, TypeVisitor visitor) {
        if (_accept(type, visitor)) {
            // check these only once at the top level 
            for (Class<?> iface : type.getInterfaces()) {
                if (!_accept(iface, visitor)) break;
            }
        }
    }

    private static boolean _accept(Class<?> type, TypeVisitor visitor) {
        if (type == null) {
            return false;
        }        
        
        boolean continueVisit = visitor.visit(type);
        if (continueVisit) {            
            for (final Field field : type.getDeclaredFields()) {
                continueVisit = visitor.visit(field);
                if (!continueVisit) break;
            }
    
            if (continueVisit) {
                for (final Method method : type.getDeclaredMethods()) {
                    continueVisit = visitor.visit(method);
                    if (!continueVisit) break;
                }
        
                if (continueVisit) {
                   continueVisit = _accept(type.getSuperclass(), visitor);
                }
            }
        }
        return continueVisit;
    }
    
    /**
     * convenience method for invoking a reflected method with no parameters and return type 'void'
     * 
     * @param noArgsMethod
     * @param instance
     * @throws IllegalAccessException
     * @throws IllegalArgumentException
     * @throws InvocationTargetException
     */
    public static void invoke(Method noArgsMethod, Object instance) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        try {
            noArgsMethod.invoke(instance);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException)cause;
            }
            else if (cause instanceof Error) {
                throw (Error)cause;
            }
            throw ite;
        }

    }

}
