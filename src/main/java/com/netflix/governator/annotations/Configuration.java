package com.netflix.governator.annotations;

import com.netflix.governator.configuration.ConfigurationProvider;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Marks a field as a configuration item. Governator will auto-assign the value based
 * on the {@link #value()} of the annotation via the set {@link ConfigurationProvider}.
 */
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Configuration
{
    /**
     * @return name/key of the config to assign
     */
    String      value();

    /**
     * @return user displayable description of this configuration
     */
    String      documentation() default "";
}
