package com.netflix.governator.conditional;

/**
 * Matcher for a specific conditional.  Matchers are created by Guice and
 * are therefore injectable.
 * @param <T>
 */
public interface Matcher<T extends Conditional> {
    /**
     * @param condition
     * @return Evaluate the conditional and return true if matched
     */
    boolean match(T condition);
}
