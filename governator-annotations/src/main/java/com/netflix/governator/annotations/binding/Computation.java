package com.netflix.governator.annotations.binding;

import javax.inject.Qualifier;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Qualifier
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
/**
 * A generic binding annotation that can be used to specify that something
 * is tied to computation related tasks. 
 * 
 * bind(ExecutorService.class).annotatedWith(Computation.class).toInstance(Executors.newCachedThreadPool(10));
 */
public @interface Computation {

}