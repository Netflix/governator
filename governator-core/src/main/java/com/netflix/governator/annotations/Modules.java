package com.netflix.governator.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.google.inject.Module;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface Modules {
    /**
     * Modules to include
     */
    Class<? extends Module>[] include() default {};
    
    /**
     * Modules to exclude
     */
    Class<? extends Module>[] exclude() default {};
    
    /**
     * Modules to force include if there is an exclude
     */
    Class<? extends Module>[] force() default {};
}
