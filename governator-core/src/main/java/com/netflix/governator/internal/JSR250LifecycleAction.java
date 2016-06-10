package com.netflix.governator.internal;

import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.governator.LifecycleAction;

public class JSR250LifecycleAction implements LifecycleAction {
    public enum ValidationMode {
        STRICT, LAX
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(JSR250LifecycleAction.class);
    private final Method method;
    private final String description;

    public JSR250LifecycleAction(Class<? extends Annotation> annotationClass, Method method) {
        this(annotationClass, method, ValidationMode.STRICT);
    }
    
    public JSR250LifecycleAction(Class<? extends Annotation> annotationClass, Method method, ValidationMode validationMode) {
        validateAnnotationUsage(annotationClass, method, validationMode);

        if (!method.isAccessible()) {
            method.setAccessible(true);
        }
        this.method = method;
        this.description = String.format("%s@%d[%s.%s()]", annotationClass.getSimpleName(),
                System.identityHashCode(this), method.getDeclaringClass().getSimpleName(), method.getName());
    }

    private void validateAnnotationUsage(Class<? extends Annotation> annotationClass, Method method, ValidationMode validationMode) {
        LOG.debug("method validationMode is " + validationMode);
        if (Modifier.isStatic(method.getModifiers())) {
            throw new IllegalArgumentException("method must not be static");
        } else if (method.getParameterCount() > 0) {
            throw new IllegalArgumentException("method parameter count must be zero");
        } else if (Void.TYPE != method.getReturnType()  && validationMode == ValidationMode.STRICT) {
            throw new IllegalArgumentException("method must have void return type");
        } else if (method.getExceptionTypes().length > 0 && validationMode == ValidationMode.STRICT) {
            for (Class<?> e : method.getExceptionTypes()) {
                if (!RuntimeException.class.isAssignableFrom(e)) {
                    throw new IllegalArgumentException(
                            "method must must not throw checked exception: " + e.getSimpleName());
                }
            }
        } else {
            int annotationCount = 0;
            for (Method m : method.getDeclaringClass().getDeclaredMethods()) {
                if (m.isAnnotationPresent(annotationClass)) {
                    annotationCount++;
                }
            }
            if (annotationCount > 1  && validationMode == ValidationMode.STRICT) {
                throw new IllegalArgumentException(
                        "declaring class must not contain multiple @" + annotationClass.getSimpleName() + " methods");
            }
        }
    }

    @Override
    public void call(Object obj) throws InvocationTargetException {
        LOG.debug("calling action {}", description);
        try {
            method.invoke(obj);
        } catch (InvocationTargetException ite) {
            Throwable cause = ite.getCause();
            if (cause instanceof RuntimeException) {
                throw (RuntimeException) cause;
            } else if (cause instanceof Error) {
                throw (Error) cause;
            }
            throw ite;
        }
        catch (IllegalAccessException | IllegalArgumentException e) {
            // extremely unlikely, as constructor sets the method to 'accessible' and validates that it takes no parameters
            throw new RuntimeException("unexpected exception in method invocation", e);
        }
     }

    @Override
    public String toString() {
        return description;
    }

}
