package com.netflix.governator.annotations;

import com.netflix.governator.lifecycle.LifecycleManager;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a method as a warm up method. Governator will execute warm up methods
 * in parallel when the {@link LifecycleManager} is started.
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface WarmUp
{
}
