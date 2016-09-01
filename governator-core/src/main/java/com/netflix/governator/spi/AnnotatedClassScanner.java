package com.netflix.governator.spi;

import com.google.inject.Binder;
import com.google.inject.Key;

import java.lang.annotation.Annotation;

/**
 * @see ScanningModuleBuidler
 */
public interface AnnotatedClassScanner {
    /**
     * @return Annotation class handled by this scanner
     */
    Class<? extends Annotation> annotationClass();
    
    /**
     * Apply the found class on the provided binder.  This can result in 0 or more bindings being
     * created
     * 
     * @param binder The binder on which to create new bindings
     * @param annotation The found annotation
     * @param key Key for the found class
     */
    <T> void applyTo(Binder binder, Annotation annotation, Key<T> key);

}
