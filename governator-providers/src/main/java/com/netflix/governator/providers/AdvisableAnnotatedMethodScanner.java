package com.netflix.governator.providers;

import com.google.common.base.Preconditions;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.ModuleAnnotatedMethodScanner;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.function.UnaryOperator;

public final class AdvisableAnnotatedMethodScanner extends ModuleAnnotatedMethodScanner {
    private static final AdvisableAnnotatedMethodScanner INSTANCE = new AdvisableAnnotatedMethodScanner();
    
    public static AdvisableAnnotatedMethodScanner scanner() {
        return INSTANCE;
    }

    public static Module asModule() {
        return new AbstractModule() {
            @Override
            protected void configure() {
                binder().scanModulesForAnnotatedMethods(AdvisableAnnotatedMethodScanner.INSTANCE);
            }
        };
    }
    
    private AdvisableAnnotatedMethodScanner() {
    }

    @Override
    public Set<? extends Class<? extends Annotation>> annotationClasses() {
        return new HashSet<>(Arrays.asList(ProvidesWithAdvice.class, Advises.class));
    }

    @Override
    public <T> Key<T> prepareMethod(Binder binder, Annotation annotation, Key<T> key, InjectionPoint injectionPoint) {
        if (annotation instanceof ProvidesWithAdvice) {
            return AdvisesBinder.getAdvisesKeyForNewItem(binder, key);
        } else if (annotation instanceof Advises) {
            Method method = (Method) injectionPoint.getMember();
            Preconditions.checkArgument(UnaryOperator.class.isAssignableFrom(method.getReturnType()), "Return type fo @Advice method must be UnaryOperator");
            ParameterizedType unaryOperatorType = (ParameterizedType) method.getGenericReturnType();
            Type type = unaryOperatorType.getActualTypeArguments()[0];
            return (Key<T>) AdvisesBinder.getAdviceKeyForNewItem(binder, key.ofType(type), ((Advises)annotation).order());
        }
        return key;
    }
}
