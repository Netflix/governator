package com.netflix.governator.providers;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @see ProvidesWithAdvice
 */
@Documented 
@Target(METHOD) 
@Retention(RUNTIME)
public @interface Advises {
    int order() default 1000;
}
