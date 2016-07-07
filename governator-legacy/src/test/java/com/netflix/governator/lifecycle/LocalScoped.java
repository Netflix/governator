package com.netflix.governator.lifecycle;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.ScopeAnnotation;

@Target({ TYPE, METHOD }) @Retention(RetentionPolicy.RUNTIME)
@ScopeAnnotation
public @interface LocalScoped {
    
}