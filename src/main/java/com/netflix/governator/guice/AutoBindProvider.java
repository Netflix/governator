package com.netflix.governator.guice;

import com.google.inject.Binder;
import com.netflix.governator.annotations.AutoBind;
import java.lang.annotation.Annotation;

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
     */
    public void     configure(Binder binder, T autoBindAnnotation);
}
