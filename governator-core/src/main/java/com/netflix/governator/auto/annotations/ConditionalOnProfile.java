package com.netflix.governator.auto.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import com.netflix.governator.auto.conditions.OnProfileCondition;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Conditional(OnProfileCondition.class)
@Deprecated
/**
 * @deprecated Moved to Karyon3
 */
public @interface ConditionalOnProfile {
    String[] value();
    
    /**
     * Match if context in all specified profiles
     * 
     * @return
     */
    boolean matchAll() default true;
}
