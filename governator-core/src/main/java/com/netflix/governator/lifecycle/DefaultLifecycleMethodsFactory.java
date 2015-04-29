package com.netflix.governator.lifecycle;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Singleton;
import javax.validation.Constraint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.inject.Inject;
import com.netflix.governator.annotations.WarmUp;
import com.netflix.governator.guice.LifecycleAnnotationProcessor;

@Singleton
public class DefaultLifecycleMethodsFactory implements LifecycleMethodsFactory {
    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Set<Class<? extends Annotation>> fieldAnnotations;
    private final Set<Class<? extends Annotation>> methodAnnotations;
    private final Set<Class<? extends Annotation>> classAnnotations;
    
    @Inject
    public DefaultLifecycleMethodsFactory(LifecycleManagerArguments arguments) {
        fieldAnnotations  = new HashSet<Class<? extends Annotation>>();
        methodAnnotations = new HashSet<Class<? extends Annotation>>();
        classAnnotations  = new HashSet<Class<? extends Annotation>>();
        
        for (LifecycleAnnotationProcessor processor : arguments.getAnnotationProcessors()) {
            fieldAnnotations .addAll(processor.getFieldAnnotations());
            methodAnnotations.addAll(processor.getMethodAnnotations());
            classAnnotations .addAll(processor.getClassAnnotations());
        }
    }
    
    @Override
    public <T> LifecycleMethods create(Class<T> clazz) {
        Multimap<Class<? extends Annotation>, Field> fieldMap = ArrayListMultimap.create();
        Multimap<Class<? extends Annotation>, Method> methodMap = ArrayListMultimap.create();;
        Multimap<Class<? extends Annotation>, Annotation> classMap = ArrayListMultimap.create();;

        AtomicBoolean hasValidations = new AtomicBoolean(false);
        
        addLifeCycleMethods(clazz, fieldMap, methodMap, classMap, hasValidations, ArrayListMultimap.<Class<? extends Annotation>, String>create());
        
        return new LifecycleMethods(fieldMap, methodMap, classMap, hasValidations.get());
    }

    private void addLifeCycleMethods(
            Class<?> clazz,
            Multimap<Class<? extends Annotation>, Field> fieldMap, 
            Multimap<Class<? extends Annotation>, Method> methodMap, 
            Multimap<Class<? extends Annotation>, Annotation> classMap,
            AtomicBoolean hasValidations,
            Multimap<Class<? extends Annotation>, String> usedNames) {
        if (clazz == null) {
            return;
        }
        
        for ( Class<? extends Annotation> annotationClass : classAnnotations ) {
            if ( clazz.isAnnotationPresent(annotationClass) ) {
                classMap.put(annotationClass, clazz.getAnnotation(annotationClass));
            }
        }

        for ( Field field : getDeclardFields(clazz) ) {
            if ( field.isSynthetic() ) {
                continue;
            }

            if ( !hasValidations.get() ) {
                hasValidations.set(checkForValidations(field));
            }

            for ( Class<? extends Annotation> annotationClass : fieldAnnotations ) {
                if (processField(field, annotationClass, usedNames)) {
                    fieldMap.put(annotationClass, field);
                }
            }
        }

        for ( Method method : getDeclaredMethods(clazz) ) {
            if ( method.isSynthetic() || method.isBridge() ) {
                continue;
            }

            for ( Class<? extends Annotation> annotationClass : methodAnnotations ) {
                if (processMethod(method, annotationClass, usedNames)) {
                    methodMap.put(annotationClass, method);
                }
            }
            
            // Special case for @WarmUp annotated methods since there is no processor for them (yet).
            if (method.isAnnotationPresent(WarmUp.class)) {
                methodMap.put(WarmUp.class, method);
            }
        }

        // Recurse through super classes and interfaces
        addLifeCycleMethods(clazz.getSuperclass(), fieldMap, methodMap, classMap, hasValidations, usedNames);
        for ( Class<?> face : clazz.getInterfaces() ) {
            addLifeCycleMethods(face, fieldMap, methodMap, classMap, hasValidations, usedNames);
        }   
    }

    private Method[] getDeclaredMethods(Class<?> clazz) {
        try {
            return clazz.getDeclaredMethods();
        }
        catch ( Throwable e ) {
            handleReflectionError(clazz, e);
        }

        return new Method[]{};
    }

    private Field[] getDeclardFields(Class<?> clazz)  {
        try {
            return clazz.getDeclaredFields();
        }
        catch ( Throwable e ) {
            handleReflectionError(clazz, e);
        }

        return new Field[]{};
    }

    private void handleReflectionError(Class<?> clazz, Throwable e) {
        if ( e != null ) {
            if ( (e instanceof NoClassDefFoundError) || (e instanceof ClassNotFoundException) ) {
                log.debug(String.format("Class %s could not be resolved because of a class path error. Governator cannot further process the class.", clazz.getName()), e);
                return;
            }

            handleReflectionError(clazz, e.getCause());
        }
    }

    private boolean checkForValidations(Field field)  {
        for ( Annotation annotation : field.getDeclaredAnnotations() ) {
            if ( annotation.annotationType().isAnnotationPresent(Constraint.class) ) {
                return true;
            }
        }
        return false;
    }

    private boolean processField(Field field, Class<? extends Annotation> annotationClass, Multimap<Class<? extends Annotation>, String> usedNames) {
        if ( field.isAnnotationPresent(annotationClass) ) {
            if ( !usedNames.get(annotationClass).contains(field.getName()) ) {
                field.setAccessible(true);
                usedNames.put(annotationClass, field.getName());
                return true;
            }
        }
        return false;
    }

    private boolean processMethod(Method method, Class<? extends Annotation> annotationClass, Multimap<Class<? extends Annotation>, String> usedNames) {
        if ( method.isAnnotationPresent(annotationClass) ) {
            if ( !usedNames.get(annotationClass).contains(method.getName()) ) {
/* TODO
                if ( method.getParameterTypes().length != 0 ) {
                    throw new UnsupportedOperationException(String.format("@PostConstruct/@PreDestroy methods cannot have arguments: %s", method.getDeclaringClass().getName() + "." + method.getName() + "(...)"));
                }
*/

                method.setAccessible(true);
                usedNames.put(annotationClass, method.getName());
                return true;
            }
        }
        return false;
    }
}
