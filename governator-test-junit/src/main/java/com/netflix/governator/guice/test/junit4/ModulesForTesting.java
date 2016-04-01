package com.netflix.governator.guice.test.junit4;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.google.inject.Module;

/**
 * Creates a Governator-Guice Injector using the provided modules for use in testing.
 * 
 * Example:
 * <pre>
 * {@literal @}RunWith(GovernatorJunit4ClassRunner.class)
 * {@literal @}ModulesForTesting({ SomeTestModule.class })
 * public class MyTestCase {
 *     
 *     {@literal @}Inject
 *     SomeDependency someDependency;
 *     
 *     {@literal @}Test
 *     public void test() {
 *        assertNotNull(someDependency);
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
@Documented
@Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface ModulesForTesting {
    
    Class<? extends Module>[] value() default {};

}
