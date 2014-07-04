package com.netflix.governator.annotations.binding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.BindingAnnotation;

/**
 * A generic binding annotation normally used to bind the DataCenter name. 
 * 
 * Internally at Netflix Datacenter is set to 'cloud' for amazon and 'dc' for a
 * traditional datacenter.  
 */
@BindingAnnotation
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface DataCenter {

}
