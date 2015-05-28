package com.netflix.governator.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Used to indicate that a constructor argument cannot be constructed concurrently.
 * Use this in conjunction with {@link ConcurrentProviders.of()} when instantiating
 * an injected dependency in parallel results in the guice @Singleton scope deadlock.
 * 
 * @see {@link ConcurrentProviders}
 * @author elandau
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER})
public @interface NonConcurrent {

}
