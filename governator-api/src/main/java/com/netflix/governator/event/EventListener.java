package com.netflix.governator.event;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented

/**
 * Marks a method as an {@link ApplicationEvent} subscriber.
 *
 * Subscriber methods should be public, void, and accept only one argument implementing {@link ApplicationEvent}
 * 
 * <code>
 * public class MyService {
 * 
 *    {@literal @}EventListener
 *    public void onEvent(MyCustomEvent event) {
 *  
 *    }
 *   
 * }
 * </code>
 *
 */
public @interface EventListener {

}
