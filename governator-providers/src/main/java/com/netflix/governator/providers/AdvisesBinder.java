package com.netflix.governator.providers;

import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.util.Types;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.function.UnaryOperator;

/**
 * AdvisesBinder is a Guice usage pattern whereby a provisioned object may be modified or even 
 * replaced using rules specified as bindings themselves. This customization is done before the 
 * provisioned object is injected. This functionality is useful for frameworks that wish to provide
 * a default implementation of a type but allow extensions and customizations by installing modules
 * with AdvisesBinder.bindAdvice() bindings.  
 * 
 * {@link Module} method annotations {@literal @}{@link ProvidesWithAdvice} and {@literal @}{@link Advises} 
 * may be used instead of calling AdvisesBinder directly.  
 * 
 * For example,
 *
 * <pre>
 * {@code
   static class AdviseList implements UnaryOperator{@literal <}List{@literal <}String{@literal >}{@literal >} {
        @Override
        public List{@literal <}String{@literal >} apply(List{@literal <}String{@literal >} t) {
            t.add("customized");
            return t;
        }
    }
    
    public static class MyModule extends AbstractModule() {
        TypeLiteral{@literal <}List{@literal <}String{@literal >}{@literal >} LIST_TYPE_LITERAL =  new TypeLiteral{@literal <}List{@literal <}String{@literal >}{@literal >}() {};
        
        @Override
        protected void configure() {
            install(AdvisableAnnotatedMethodScanner.asModule());
            
            AdvisesBinder.bind(binder(), LIST_TYPE_LITERAL).toInstance(new ArrayList{@literal <}{@literal >}());
            AdvisesBinder.bindAdvice(binder(), LIST_TYPE_LITERAL, 0).to(AdviseList.class);
        }
    });
    }

 * 
 * will add "customized" to the empty list.  When List&ltString&gt is finally injected it'll have
 * ["customized"].
 * 
 * Note that AdvisesBinder can be used with qualifiers such as {@literal @}Named.
 */
public abstract class AdvisesBinder {
    private AdvisesBinder() {
    }

    public static <T> LinkedBindingBuilder<T> bind(Binder binder, TypeLiteral<T> type) {
        return newRealAdvisesBinder(binder, Key.get(type));
    }

    public static <T> LinkedBindingBuilder<T> bind(Binder binder, Class<T> type) {
        return newRealAdvisesBinder(binder, Key.get(type));
    }

    public static <T> LinkedBindingBuilder<T> bind(Binder binder, TypeLiteral<T> type, Annotation annotation) {
        return newRealAdvisesBinder(binder, Key.get(type, annotation));
    }

    public static <T> LinkedBindingBuilder<T> bind(Binder binder, Class<T> type, Annotation annotation) {
        return newRealAdvisesBinder(binder, Key.get(type, annotation));
    }

    public static <T> LinkedBindingBuilder<T> bind(Binder binder, TypeLiteral<T> type, Class<? extends Annotation> annotationType) {
        return newRealAdvisesBinder(binder, Key.get(type, annotationType));
    }

    public static <T> LinkedBindingBuilder<T> bind(Binder binder, Key<T> key) {
        return newRealAdvisesBinder(binder, key);
    }
    
    public static <T> LinkedBindingBuilder<T> bind(Binder binder, Class<T> type,  Class<? extends Annotation> annotationType) {
        return newRealAdvisesBinder(binder, Key.get(type, annotationType));
    }
    
    static <T> Key<T> getAdvisesKeyForNewItem(Binder binder, Key<T> key) {
        binder = binder.skipSources(AdvisesBinder.class);
        
        Annotation annotation = key.getAnnotation();
        String elementName = key.hasAttributes() ? key.getAnnotation().toString() : "";
        AdviceElement element = new AdviceElementImpl(elementName, AdviceElement.Type.SOURCE, 0);
        Key<T> uniqueKey = Key.get(key.getTypeLiteral(), element);
        
        // Bind the original key to a new AdvisedProvider
        binder.bind(key).toProvider(new AdvisedProvider<T>(key.getTypeLiteral(), element.name(), annotation, binder.getProvider(uniqueKey)));

        return uniqueKey;
    }

    static <T> LinkedBindingBuilder<T> newRealAdvisesBinder(Binder binder, Key<T> key) {
        Key<T> uniqueKey = getAdvisesKeyForNewItem(binder, key);
        return binder.bind(uniqueKey);
    }
    
    public static <T> LinkedBindingBuilder<UnaryOperator<T>> bindAdvice(Binder binder, TypeLiteral<T> type, int order) {
        return newRealAdviceBinder(binder, Key.get(type), order);
    }

    public static <T> LinkedBindingBuilder<UnaryOperator<T>> bindAdvice(Binder binder, Class<T> type, int order) {
        return newRealAdviceBinder(binder, Key.get(type), order);
    }

    public static <T> LinkedBindingBuilder<UnaryOperator<T>> bindAdvice(Binder binder, TypeLiteral<T> type, Annotation annotation, int order) {
        return newRealAdviceBinder(binder, Key.get(type, annotation), order);
    }

    public static <T> LinkedBindingBuilder<UnaryOperator<T>> bindAdvice(Binder binder, Class<T> type, Annotation annotation, int order) {
        return newRealAdviceBinder(binder, Key.get(type, annotation), order);
    }

    public static <T> LinkedBindingBuilder<UnaryOperator<T>> bindAdvice(Binder binder, TypeLiteral<T> type, Class<? extends Annotation> annotationType, int order) {
        return newRealAdviceBinder(binder, Key.get(type, annotationType), order);
    }

    public static <T> LinkedBindingBuilder<UnaryOperator<T>> bindAdvice(Binder binder, Key<T> key, int order) {
        return newRealAdviceBinder(binder, key, order);
    }
    
    public static <T> LinkedBindingBuilder<UnaryOperator<T>> bindAdvice(Binder binder, Class<T> type,  Class<? extends Annotation> annotationType, int order) {
        return newRealAdviceBinder(binder, Key.get(type, annotationType), order);
    }
    
    @SuppressWarnings("unchecked")
    static <T> Key<UnaryOperator<T>> getAdviceKeyForNewItem(Binder binder, Key<T> key, int order) {
        binder = binder.skipSources(AdvisesBinder.class);
        String elementName = key.hasAttributes() ? key.getAnnotation().toString() : "";
        @SuppressWarnings("unused")
        Annotation annotation = key.getAnnotation();
        
        Type adviceType = Types.newParameterizedType(UnaryOperator.class, key.getTypeLiteral().getType());
        return (Key<UnaryOperator<T>>) Key.get(adviceType, new AdviceElementImpl(elementName, AdviceElement.Type.ADVICE, order));
    }

    static <T> LinkedBindingBuilder<UnaryOperator<T>> newRealAdviceBinder(Binder binder, Key<T> key, int order) {
        Key<UnaryOperator<T>> uniqueKey = getAdviceKeyForNewItem(binder, key, order);
        return binder.bind(uniqueKey);
    }
}
