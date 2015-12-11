package com.netflix.governator.conditional;


/**
 * Contract for any conditional that may be applied to a conditional binding
 * bound via {@link ConditionalBinder}.
 */
public interface Conditional<T extends Conditional<T>> {
    /**
     * @return Class that can process this conditional.  This class
     * is instantiated by Guice and is therefore injectable.  The class should be 
     * annotated as a {@literal @}Singleton 
     */
    Class<? extends Matcher<T>> getMatcherClass();
}
