package com.netflix.governator.providers;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.HasDependencies;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderWithExtensionVisitor;
import com.google.inject.spi.Toolable;
import com.google.inject.util.Types;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.UnaryOperator;

import javax.inject.Provider;

class AdvisedProvider<T> implements ProviderWithExtensionVisitor<T>, HasDependencies  {
    private final Set<Dependency<?>> dependencies = new HashSet<>();
    private final String name;
    private final Provider<T> delegate;
    private final List<ProvisionAdviceHolder<UnaryOperator<T>>> adviceBindings = new ArrayList<>();
    private final TypeLiteral<UnaryOperator<T>> advisesType;
    
    public AdvisedProvider(TypeLiteral<T> typeLiteral, String name, Annotation annotation, Provider<T> delegate) {
        this.name = name;
        this.delegate = delegate;
        this.advisesType = (TypeLiteral<UnaryOperator<T>>) TypeLiteral.get(Types.newParameterizedType(UnaryOperator.class, typeLiteral.getType()));
    }
    
    @Override
    public T get() {
        return adviceBindings
            .stream()
            .map(advice -> advice.binding.getProvider().get())
            .reduce(delegate.get(), 
                    (advised, advice) -> advice.apply(advised),
                    (current, next) -> next);
    }

    @Override
    public Set<Dependency<?>> getDependencies() {
        return dependencies;
    }

    @Override
    public <B, V> V acceptExtensionVisitor(BindingTargetVisitor<B, V> visitor,
            ProviderInstanceBinding<? extends B> binding) {
        return visitor.visit(binding);
    }

    @SuppressWarnings("unchecked")
    @Toolable
    @javax.inject.Inject
    protected void initialize(Injector injector) {
        for (Binding<?> binding : injector.findBindingsByType(advisesType)) {
            Key<?> bindingKey = binding.getKey();
            if (bindingKey.hasAttributes() && AdviceElement.class.isAssignableFrom(bindingKey.getAnnotationType())) {
                AdviceElementImpl adviceElement = (AdviceElementImpl) bindingKey.getAnnotation();
                if (name.equals(adviceElement.name())) {
                    if (adviceElement.type() == AdviceElement.Type.ADVICE) {
                        adviceBindings.add(new ProvisionAdviceHolder<UnaryOperator<T>>((Binding<UnaryOperator<T>>) binding, adviceElement.getOrder()));
                    }
                    dependencies.add(Dependency.get(bindingKey));
                }
            }
        }
        
        adviceBindings.sort(ByOrder);
    }
    
    static Comparator<ProvisionAdviceHolder<?>> ByOrder = new Comparator<ProvisionAdviceHolder<?>>() {
        @Override
        public int compare(ProvisionAdviceHolder<?> o1, ProvisionAdviceHolder<?> o2) {
            int rv =  Integer.compare(o1.order, o2.order);
            if (rv == 0) {
                return Integer.compare(System.identityHashCode(o1.hashCode()), System.identityHashCode(o2.hashCode()));
            }
            return rv;
        }
    };
    
    static class ProvisionAdviceHolder<T>  {
        Binding<T> binding;
        int order;
        
        public ProvisionAdviceHolder(Binding<T> binding, int order) {
            this.order = order;
            this.binding = binding;
        }
    }
}