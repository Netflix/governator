package com.netflix.governator.lifecycle;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.Set;

public class EmptyClasspathScanner extends ClasspathScanner {
    @Override
    public Set<Class<?>> getClasses() {
        return Collections.emptySet();
    }

    @Override
    public Set<Constructor> getConstructors() {
        return Collections.emptySet();
    }

    @Override
    public Set<Method> getMethods() {
        return Collections.emptySet();
    }

    @Override
    public Set<Field> getFields() {
        return Collections.emptySet();
    }
}
