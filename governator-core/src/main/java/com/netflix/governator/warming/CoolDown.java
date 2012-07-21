package com.netflix.governator.warming;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface CoolDown
{
    boolean canBeParallel() default false;
}
