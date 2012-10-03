package com.netflix.governator.guice;

import com.google.inject.Binder;
import com.netflix.governator.annotations.AutoBind;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Used to perform the binding for a given {@link AutoBind} annotation
 */
public interface AutoBindProvider<T extends Annotation>
{
    /**
     * Called for auto binding of constructor arguments
     *
     * @param binder the Guice binder
     * @param autoBindAnnotation the @AutoBind or custom annotation
     * @param constructor the constructor
     * @param argumentIndex the 0 based index of the parameter being processed
     */
    public void     configureForConstructor(Binder binder, T autoBindAnnotation, Constructor constructor, int argumentIndex);

    /**
     * Called for auto binding of method arguments
     *
     * @param binder the Guice binder
     * @param autoBindAnnotation the @AutoBind or custom annotation
     * @param method the method
     * @param argumentIndex the 0 based index of the parameter being processed
     */
    public void     configureForMethod(Binder binder, T autoBindAnnotation, Method method, int argumentIndex);

    /**
     * Called for auto binding of a field
     *
     * @param binder the Guice binder
     * @param autoBindAnnotation the @AutoBind or custom annotation
     * @param field the field
     */
    public void     configureForField(Binder binder, T autoBindAnnotation, Field field);
}
