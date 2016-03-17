package com.netflix.governator.guice.test;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.mockito.Answers;

@Target(FIELD)
@Retention(RUNTIME)
@Documented
public @interface ReplaceWithMock {
    Answers answer() default Answers.RETURNS_DEFAULTS;

    String name() default "";

}
