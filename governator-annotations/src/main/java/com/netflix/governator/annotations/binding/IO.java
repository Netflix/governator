package com.netflix.governator.annotations.binding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

@Qualifier
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
/**
 * A generic binding annotation that can be used to specify that something
 * is tied to IO processing.  For example, 
 * 
 * bind(ExecutorService.class).annotatedWith(IO.class).toInstance(Executors.newScheduledThreadPool(10));
 * 
 */
public @interface IO
{
}