package com.netflix.governator.guice.lazy;

import com.google.inject.ScopeAnnotation;
import com.google.inject.Scopes;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Same as {@link LazySingleton} with the addition of allowing for more
 * concurrency. The various {@link Scopes#SINGLETON} based scopes have
 * a major concurrency restriction due to a blunt synchronization (see
 * the comment inside of the Guice code). This version synchronizes
 * on the object key and, thus, can construct multiple types of singletons
 * concurrently.
 * 
 * @deprecated Use javax.inject.Singleton instead.  FineGrainedLazySingleton is not needed 
 * as of Guice4 which fixes the global lock issue.
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@ScopeAnnotation
public @interface FineGrainedLazySingleton
{
}
