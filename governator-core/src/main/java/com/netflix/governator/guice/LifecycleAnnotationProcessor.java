package com.netflix.governator.guice;

import java.lang.annotation.Annotation;
import java.util.Collection;

import com.netflix.governator.lifecycle.LifecycleMethods;
import com.netflix.governator.lifecycle.LifecycleState;

/**
 * Plugin to LifecycleManager which identifies annotations to be processed as well 
 * as a method through which they are processed.  As Guice hears about classes 
 * LifecycleManager which check if the class contains any annotations specified in 
 * any of the configuration LifecycleAnnotationProcessor's and then enables 
 * annotation processing (via. the process() methods) after a new instance is 
 * injected.
 * 
 * @author elandau
 *
 */
public interface LifecycleAnnotationProcessor {
    /**
     * Process annotations as part of lifecycle management for the provided object
     * @param obj
     * @param methods
     * @throws Exception
     */
    void process(Object obj, LifecycleMethods methods) throws Exception;
    
    /**
     * Return Field annotations that are to be processed by this LifecycleAnnotationProcessor
     * @return
     */
    Collection<Class<? extends Annotation>> getFieldAnnotations();

    /**
     * Return Method annotations that are to be processed by this LifecycleAnnotationProcessor
     * @return
     */
    Collection<Class<? extends Annotation>> getMethodAnnotations();
    
    /**
     * Return Class annotations that are to be processed by this LifecycleAnnotationProcessor
     * @return
     */
    Collection<Class<? extends Annotation>> getClassAnnotations();
    
    /**
     * Return the lifecycle state with which this processor is associated
     * @return
     */
    LifecycleState getState();
}
