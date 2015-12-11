package com.netflix.governator.conditional;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;

import com.google.inject.BindingAnnotation;

/**
 * Unique qualifier associated with conditional bindings.  The unique qualifier
 * is used to ensure that the bound element isn't bound to a real key that user
 * code would inject.
 */
@Retention(RUNTIME) 
@BindingAnnotation
@interface ConditionalElement {
    String keyName();
    int uniqueId();
}
