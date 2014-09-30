package com.netflix.governator.guice;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderWithExtensionVisitor;
import com.google.inject.spi.Toolable;

/**
 * Specialized {@link Provider} for an existing object instance that 
 * will force member injection before injecting the instance
 * @author elandau
 *
 */
class MemberInjectingInstanceProvider<T> implements ProviderWithExtensionVisitor<T> {

    private final T module;

    public MemberInjectingInstanceProvider(T module) {
        this.module = module;
    }
    
    @Override
    public T get() {
        return module;
    }

    @Override
    public <B, V> V acceptExtensionVisitor(
            BindingTargetVisitor<B, V> visitor,
            ProviderInstanceBinding<? extends B> binding) {
        return visitor.visit(binding);
    }
    
    @Inject
    @Toolable
    void initialize(Injector injector) {
        injector.injectMembers(module);
    }
}
