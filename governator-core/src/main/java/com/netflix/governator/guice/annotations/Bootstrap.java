package com.netflix.governator.guice.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.netflix.governator.guice.BootstrapModule;

@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Bootstrap {
    Class<? extends BootstrapModule> value();
}
