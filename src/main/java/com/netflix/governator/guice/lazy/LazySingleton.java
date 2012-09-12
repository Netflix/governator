package com.netflix.governator.guice.lazy;

import com.google.inject.ScopeAnnotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Scope annotation that marks a class as singleton that should NOT be
 * allocated eagerly
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ScopeAnnotation
public @interface LazySingleton
{
}
