package com.netflix.governator.providers;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * {@literal @}ProvidesWithAdvice is a Guice usage pattern whereby a method of a module can be 
 * annotated with {@literal @}ProvidesWithAdvice to return the initial version of an object and then
 * have {@literal @}Advice annotated methods that can modify the object.  All of this happends BEFORE
 * the final object is injected.  This functionality is useful for frameworks that wish to provide
 * a default implementation of a type but allow extensions and customizations by installing modules
 * with {@literal @}Advice annotated methods.
 * 
 * For example,
 *
 * <pre>
 * {@literal @}ProvidesWithAdvice
 * List<String> provideFoo() { return Arrays.asList("a", "b", "c"); }
 *
 * {@literal @}Advice
 * ProvisionAdvice<List<String>> addMoreToFoo() { return list -> list.add("d"); }
 * </pre>
 *
 * will add "d" to the original ["a", "b", "c"].  When List&ltString&gt is finally injected it'll have
 * ["a", "b", "c", "d"].
 * 
 * Note that {@literal @}ProvideWithAdvice can be used with qualifiers such as {@literal @}Named 
 * with matching qualifiers added to the {@literal @}Advice methods.
 * 
 */
@Documented 
@Target(METHOD) 
@Retention(RUNTIME)
public @interface ProvidesWithAdvice {

}
