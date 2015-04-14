package com.netflix.governator.lifecycle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Set;

public abstract class ClasspathScanner {
    public abstract Set<Class<?>> getClasses();
    public abstract Set<Constructor> getConstructors();
    public abstract Set<Method> getMethods();
    public abstract Set<Field> getFields();
}
