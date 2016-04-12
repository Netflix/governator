package com.netflix.governator.guice.test.junit4;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.mockito.Spy;

import com.google.inject.BindingAnnotation;

/**
 * Wraps an existing binding with a Mockito {@link Spy}.
 * 
 * Example:
 * <pre>
 * {@literal @}RunWith(GovernatorJunit4ClassRunner.class)
 * {@literal @}ModulesForTesting({ SomeTestModule.class })
 * public class MyTestCase {
 *     
 *     {@literal @}Inject {@literal @}WrapWithSpy
 *     SomeDependency someDependency;
 *     
 *     {@literal @}Test
 *     public void test() {
 *        someDependency.doSomething();
 *        verify(someDependency, times(1)).doSomething();
 *    }
 * }
 *     
 * public class SomeTestModule extends AbstractModule {
 * 
 *         {@literal @}Override
 *         protected void configure() {
 * 
 *             bind(SomeDependency.class);
 *         }
 * 
 *     }
 * }
 * </pre>
 */
@Target(FIELD)
@Retention(RUNTIME)
@Documented
public @interface WrapWithSpy {

    String name() default "";
    
}
