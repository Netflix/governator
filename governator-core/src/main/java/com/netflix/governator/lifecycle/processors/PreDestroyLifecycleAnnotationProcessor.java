package com.netflix.governator.lifecycle.processors;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.governator.guice.LifecycleAnnotationProcessor;
import com.netflix.governator.lifecycle.LifecycleMethods;
import com.netflix.governator.lifecycle.LifecycleState;

@Singleton
public class PreDestroyLifecycleAnnotationProcessor implements LifecycleAnnotationProcessor {
    private static final Logger log = LoggerFactory.getLogger(PostConstructLifecycleAnnotationProcessor.class);

    @Override
    public void process(Object obj, LifecycleMethods methods) throws Exception {
        for (Method method : methods.methodsFor(PreDestroy.class)) {
            log.debug(String.format("\t%s()", method.getName()));
            try {
                method.invoke(obj);
            }
            catch ( Throwable e ) {
                log.error("Couldn't stop lifecycle managed instance", e);
            }
        }
    }

    @Override
    public Collection<Class<? extends Annotation>> getFieldAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public Collection<Class<? extends Annotation>> getMethodAnnotations() {
        return Collections.unmodifiableList(Arrays.<Class<? extends Annotation>>asList(PreDestroy.class));
    }

    @Override
    public Collection<Class<? extends Annotation>> getClassAnnotations() {
        return Collections.emptyList();
    }

    @Override
    public LifecycleState getState() {
        return LifecycleState.PRE_DESTROYING;
    }

}