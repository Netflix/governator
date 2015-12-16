package com.netflix.governator.annotations.binding;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * A generic binding annotation that can be associated with the up status
 * of an application.
 *
 * <pre>
 *  bind(Boolean.class).annotatedWith(UpStatus.class).toInstance(new AtomicBoolean(true));
 *  bind(new TypeLiteral<Supplier<Boolean>() {}>).annotatedWith(UpStatus.class).toInstance(new SomeSupplierThatTellsYouTheUpStatus());
 *
 *  public class Foo() {
 *     &#64;Inject
 *     public Foo(@UpStatus Supplier<Boolean> isUp) {
 *        System.out.println("Application isUp: " + isUp);
 *     }
 *  }
 * </pre>
 *
 * If you're using RxJava you can set up an Observable of up status
 * <pre>
 *  bind(new TypeLiteral<Observable<Boolean>>() {}>).annotatedWith(UpStatus.class).toInstance(new SomethingThatEmitsChangesInUpStatus());
 *
 *  public class Foo() {
 *     &#64;Inject
 *     public Foo(@UpStatus Observable<Boolean> upStatus) {
 *         upStatus.subscribe(new Action1<Boolean>() {
 *             public void call(Boolean status) {
 *                 System.out.println("Status is now up");
 *             }
 *         });
 *     }
 *  }
 *
 * @see DownStatus
 * </pre>
 */
@Qualifier
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface UpStatus
{
}
