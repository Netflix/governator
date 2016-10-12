package com.netflix.governator;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import java.lang.annotation.Retention;
import static java.lang.annotation.RetentionPolicy.RUNTIME;
import java.lang.annotation.Target;

import com.google.inject.ScopeAnnotation;

/**
 * annotation for binding instances bound to ThreadLocalScope
 *
 */
@Target({ TYPE, METHOD }) @Retention(RUNTIME) @ScopeAnnotation
public @interface ThreadLocalScoped {}