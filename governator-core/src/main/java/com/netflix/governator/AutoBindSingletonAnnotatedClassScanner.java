package com.netflix.governator;

import com.google.common.base.Preconditions;
import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.ScopeAnnotation;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.ScopedBindingBuilder;
import com.google.inject.internal.MoreTypes;
import com.google.inject.multibindings.Multibinder;
import com.netflix.governator.annotations.AutoBindSingleton;
import com.netflix.governator.guice.lazy.LazySingletonScope;
import com.netflix.governator.spi.AnnotatedClassScanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Set;

import javax.inject.Scope;
import javax.inject.Singleton;

public class AutoBindSingletonAnnotatedClassScanner implements AnnotatedClassScanner {
    private static final Logger LOG = LoggerFactory.getLogger(AutoBindSingletonAnnotatedClassScanner.class);
    
    @Override
    public Class<? extends Annotation> annotationClass() {
        return AutoBindSingleton.class;
    }

    @Override
    public <T> void applyTo(Binder binder, Annotation annotation, Key<T> key) {
        AutoBindSingleton abs = (AutoBindSingleton)annotation;
        Class clazz = key.getTypeLiteral().getRawType();
        if ( Module.class.isAssignableFrom(clazz) ) {
            try {
                binder.install((Module)clazz.newInstance());
            } catch (Exception e) {
                binder.addError("Failed to install @AutoBindSingleton module " + clazz.getName());
                binder.addError(e);
            }
        } else {
            bindAutoBindSingleton(binder, abs, clazz);
        }
    }

    private void bindAutoBindSingleton(Binder binder, AutoBindSingleton annotation, Class<?> clazz) {
        LOG.info("Installing @AutoBindSingleton '{}'", clazz.getName());
        LOG.info("***** @AutoBindSingleton for '{}' is deprecated as of 2015-10-10.\nPlease use a Guice module with bind({}.class).asEagerSingleton() instead.\nSee https://github.com/Netflix/governator/wiki/Auto-Binding", 
                clazz.getName(), clazz.getSimpleName() );
        
        Singleton singletonAnnotation = clazz.getAnnotation(Singleton.class);
        if (singletonAnnotation == null) {
            LOG.info("***** {} should also be annotated with @Singleton to ensure singleton behavior", clazz.getName());
        }
        
        Class<?> annotationBaseClass = getAnnotationBaseClass(annotation);
        
        // Void.class is used as a marker to mean "default" because annotation defaults cannot be null
        if ( annotationBaseClass != Void.class ) {
            Object foundBindingClass = searchForBaseClass(clazz, annotationBaseClass, Sets.newHashSet());
            Preconditions.checkArgument(foundBindingClass != null, String.format("AutoBindSingleton class %s does not implement or extend %s", clazz.getName(), annotationBaseClass.getName()));

            if ( foundBindingClass instanceof Class ) {
                if ( annotation.multiple() ) {
                    Multibinder<?> multibinder = Multibinder.newSetBinder(binder, (Class)foundBindingClass);
                    //noinspection unchecked
                    applyScope(multibinder
                            .addBinding()
                            .to((Class)clazz), 
                        clazz, annotation);
                } else {
                    //noinspection unchecked
                    applyScope(binder
                            .withSource(getCurrentStackElement())
                            .bind((Class)foundBindingClass)
                            .to(clazz),
                        clazz, annotation);
                }
            }
            else if ( foundBindingClass instanceof Type ) {
                TypeLiteral typeLiteral = TypeLiteral.get((Type)foundBindingClass);
                if ( annotation.multiple() ) {
                    //noinspection unchecked
                    applyScope(Multibinder.newSetBinder(binder, typeLiteral)
                            .addBinding()
                            .to((Class)clazz),
                        clazz, annotation);
                } else {
                    //noinspection unchecked
                    applyScope(binder
                            .withSource(getCurrentStackElement())
                            .bind(typeLiteral).to(clazz), 
                        clazz, annotation);
                }
            } else {
                binder.addError("Unexpected binding class: " + foundBindingClass);
            }
        }
        else {
            Preconditions.checkState(!annotation.multiple(), "@AutoBindSingleton(multiple=true) must have either value or baseClass set");
            applyScope(binder
                    .withSource(getCurrentStackElement())
                    .bind(clazz), 
                clazz, annotation);
        }
    }
    
    private StackTraceElement getCurrentStackElement() {
        return Thread.currentThread().getStackTrace()[1];
    }

    private void applyScope(ScopedBindingBuilder builder, Class<?> clazz, AutoBindSingleton annotation) {
        if (hasScopeAnnotation(clazz)) {
            // Honor scoped annotations first
        } else if (annotation.eager()) {
            builder.asEagerSingleton();
        } else  {
            builder.in(LazySingletonScope.get());
        }
    }
    
    private boolean hasScopeAnnotation(Class<?> clazz) {
        Annotation scopeAnnotation = null;
        for (Annotation annot : clazz.getAnnotations()) {
            if (annot.annotationType().isAnnotationPresent(ScopeAnnotation.class) || annot.annotationType().isAnnotationPresent(Scope.class)) {
                Preconditions.checkState(scopeAnnotation == null, "Multiple scopes not allowed");
                scopeAnnotation = annot;
            }
        }
        return scopeAnnotation != null;
    }
    
    private Class<?> getAnnotationBaseClass(AutoBindSingleton annotation) {
        Class<?> annotationValue = annotation.value();
        Class<?> annotationBaseClass = annotation.baseClass();
        Preconditions.checkState((annotationValue == Void.class) || (annotationBaseClass == Void.class), "@AutoBindSingleton cannot have both value and baseClass set");

        return (annotationBaseClass != Void.class) ? annotationBaseClass : annotationValue;
    }

    private Object searchForBaseClass(Class<?> clazz, Class<?> annotationBaseClass, Set<Object> usedSet) {
        if (clazz == null) {
            return null;
        }

        if (clazz.equals(annotationBaseClass)) {
            return clazz;
        }

        if (!usedSet.add(clazz)) {
            return null;
        }

        for (Type type : clazz.getGenericInterfaces()) {
            if (MoreTypes.getRawType(type).equals(annotationBaseClass)) {
                return type;
            }
        }

        if (clazz.getGenericSuperclass() != null) {
            if (MoreTypes.getRawType(clazz.getGenericSuperclass()).equals(annotationBaseClass)) {
                return clazz.getGenericSuperclass();
            }
        }

        for (Class<?> interfaceClass : clazz.getInterfaces()) {
            Object foundBindingClass = searchForBaseClass(interfaceClass, annotationBaseClass, usedSet);
            if (foundBindingClass != null) {
                return foundBindingClass;
            }
        }

        return searchForBaseClass(clazz.getSuperclass(), annotationBaseClass, usedSet);
    }
}
