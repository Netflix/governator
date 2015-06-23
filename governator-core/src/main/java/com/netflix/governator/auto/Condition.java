package com.netflix.governator.auto;

/**
 * Module condition that must be paired with a Conditional annotation
 * 
 * @author elandau
 *
 * @param <T>
 */
public interface Condition<T> {
    /**
     * Check if the Condition is true.  
     * 
     * @param param  The annotation instance
     * @return
     */
    boolean check(T param);
}
