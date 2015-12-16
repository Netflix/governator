package com.netflix.governator.conditional;

import com.google.inject.Injector;

/**
 * Contract for any conditional that may be applied to a conditional binding
 * bound via {@link ConditionalBinder}.
 * 
 * Dependencies needed by a concrete conditional should be injected using 
 * member injection.
 * 
 */
public interface Conditional {
    /**
     * Evaluate whether the condition is true.  evaluate() is only called once at injector
     * creation time.
     * @return True if conditional is true otherwise false
     */
    boolean matches(Injector injector);
}
