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
@Bootstrap(bootstrap=GovernatorBootstrap.class)
public @interface GovernatorConfiguration {
    /**
     * Turn on class path scanning for @AutoBindSingleton
     * @return
     */
    boolean enableAutoBindSingleton() default false;

    /**
     * Change the {@link Stage} for the main injector.  By default we use DEVELOPMENT
     * stage is it provides the most deterministic behavior for transitive lazy
     * singleton instantiation.
     * @return
     */
    Stage stage() default Stage.DEVELOPMENT;

    /**
     * Simulated child injectors is the prefered mode but can be changed here 
     * back to REAL_CHILD_INJECTORS.
     * @return
     */
    LifecycleInjectorMode mode() default LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS;

    /**
     * Actions to perform after the injector is created
     * @return
     */
    Class<? extends PostInjectorAction>[] actions() default {};

    /**
     * {@link ModuleTransform} operations to perform on the final list of modules
     * @return
     */
    Class<? extends ModuleTransformer>[] transformers() default {};
}
