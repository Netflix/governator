package com.netflix.governator.auto.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.netflix.governator.auto.conditions.OnSystemCondition;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnSystemCondition.class)
@Deprecated
/**
 * @deprecated Moved to Karyon3
 */
public @interface ConditionalOnSystem {
    String name();
    String value();
}
