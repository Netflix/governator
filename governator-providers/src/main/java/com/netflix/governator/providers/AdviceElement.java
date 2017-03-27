package com.netflix.governator.providers;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import com.google.inject.BindingAnnotation;

import java.lang.annotation.Retention;

@Retention(RUNTIME) 
@BindingAnnotation
@interface AdviceElement {
    enum Type {
        SOURCE, ADVICE
    }
    
    /**
     * Unique ID that so multiple @Advice with the same return type may be defined without
     * resulting in a duplicate binding exception.
     */
    int uniqueId();
    
    /**
     * Name derived from a toString() of a qualifier and is used to match @Advice annotated method
     * with their @AdviceProvision
     */
    String name();
    
    AdviceElement.Type type();
}