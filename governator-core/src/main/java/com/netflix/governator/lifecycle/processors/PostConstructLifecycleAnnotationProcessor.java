package com.netflix.governator.lifecycle.processors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Singleton;
import com.netflix.governator.guice.LifecycleAnnotationProcessor;
import com.netflix.governator.lifecycle.LifecycleMethods;
import com.netflix.governator.lifecycle.LifecycleState;

@Singleton
public class PostConstructLifecycleAnnotationProcessor implements LifecycleAnnotationProcessor {
    private static final Logger log = LoggerFactory.getLogger(PostConstructLifecycleAnnotationProcessor.class);

    @Override
    public void process(Object obj, LifecycleMethods methods) throws Exception {
        for (Method method : methods.methodsFor(PostConstruct.class)) {
            log.debug(String.format("\t%s()", method.getName()));
            method.invoke(obj);
        }
    }

    @Override
    public Collection<Class<? extends Annotation>> getFieldAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Class<? extends Annotation>> getMethodAnnotations() {
        return Collections.unmodifiableList(Arrays.<Class<? extends Annotation>>asList(PostConstruct.class));
    }

    @Override
    public Collection<Class<? extends Annotation>> getClassAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public LifecycleState getState() {
        return LifecycleState.POST_CONSTRUCTING;
    }

}
