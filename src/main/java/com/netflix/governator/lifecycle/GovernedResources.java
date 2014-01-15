package com.netflix.governator.lifecycle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * Representation of all resources that are to be managed by Governator for
 * all auto bind and configuration operations.
 * 
 * The default implementation, ClasspathScanner, uses classpath scanning to 
 * discover all resources to be governed.  A custom implementation
 * may be provided but this implementation must properly implement all
 * methods for governated components to behave in a consistent manner.
 * 
 * @author elandau
 */
public interface GovernedResources {
    public Set<Class<?>> getClasses();

    public Set<Constructor> getConstructors();

    public Set<Method> getMethods();

    public Set<Field> getFields();
}