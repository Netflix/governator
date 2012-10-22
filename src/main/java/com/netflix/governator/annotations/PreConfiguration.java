package com.netflix.governator.annotations;

import com.netflix.governator.lifecycle.LifecycleManager;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marks a method as a pre-configuration method. Governator will execute pre-configuration methods
 * prior to configuration assignment
 */
@Documented
@Retention(RUNTIME)
@Target(METHOD)
public @interface PreConfiguration
{
}
