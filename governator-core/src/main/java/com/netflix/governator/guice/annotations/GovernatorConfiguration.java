package com.netflix.governator.guice.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.Stage;
import com.netflix.governator.guice.LifecycleInjectorMode;
import com.netflix.governator.guice.ModuleTransformer;
import com.netflix.governator.guice.PostInjectorAction;
import com.netflix.governator.guice.bootstrap.GovernatorBootstrap;

/**
 * Governator configuration for the main bootstrap class with 'good' default
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
@Bootstrap(GovernatorBootstrap.class)
public @interface GovernatorConfiguration {
    boolean enableAutoBindSingleton() default false;

    Stage stage() default Stage.DEVELOPMENT;

    LifecycleInjectorMode mode() default LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS;

    Class<? extends PostInjectorAction>[] actions() default {};

    Class<? extends ModuleTransformer>[] transformers() default {};
}
