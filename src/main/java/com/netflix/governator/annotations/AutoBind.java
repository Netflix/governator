package com.netflix.governator.annotations;

import com.google.inject.BindingAnnotation;
import com.netflix.governator.guice.AutoBindProvider;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Annotation binding that combines with Governator's classpath scanning and a bound
 * {@link AutoBindProvider} to automatically/programmatically bind fields and constructor/method
 * arguments
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.CONSTRUCTOR, ElementType.METHOD, ElementType.ANNOTATION_TYPE, ElementType.PARAMETER})
@BindingAnnotation
public @interface AutoBind
{
    /**
     * @return optional binding argument
     */
    String      value() default "";
}
