package com.netflix.governator.guice.test.junit4;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import org.mockito.Answers;

/**
 * Creates an override binding for the annotated type. 
 * 
 * Example:
 * <pre>
 * {@literal @}RunWith(GovernatorJunit4ClassRunner.class)
 * {@literal @}ModulesForTesting({ SomeTestModule.class })
 * public class MyTestCase {
 *     
 *     {@literal @}Inject {@literal @}ReplaceWithMock
 *     SomeDependency someDependency;
 *     
 *     {@literal @}Test
 *     public void test() {
 *        Mockito.when(someDependency.doSomething()).thenReturn("something");
 *        assertEquals("something", someDependency.doSomething());
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
public @interface ReplaceWithMock {
    
    /**
     * Sets the configured answer of this mock.
     */
    Answers answer() default Answers.RETURNS_DEFAULTS;
    
    /**
     * The name of the binding you wish to mock.
     */
    String name() default "";

}
