package com.netflix.governator.guice.test;

import java.lang.reflect.Constructor;

import com.google.inject.Scopes;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.ConstructorBinding;
import com.google.inject.spi.ConvertedConstantBinding;
import com.google.inject.spi.ExposedBinding;
import com.google.inject.spi.InstanceBinding;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.ProviderBinding;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderKeyBinding;
import com.google.inject.spi.UntargettedBinding;

class CopyBindingTargetVisitor<T> implements BindingTargetVisitor<T, Void> {
    
    private LinkedBindingBuilder<T> builder;

    public CopyBindingTargetVisitor(LinkedBindingBuilder<T> builder) {
        this.builder = builder;
    }

    @Override
    public Void visit(InstanceBinding<? extends T> binding) {
        builder.toInstance(binding.getInstance());
        return null;
    }

    @SuppressWarnings("deprecation")
    @Override
    public Void visit(ProviderInstanceBinding<? extends T> binding) {
        builder.toProvider(binding.getProviderInstance()).in(Scopes.SINGLETON);
        return null;
    }

    @Override
    public Void visit(ProviderKeyBinding<? extends T> binding) {
        builder.toProvider(binding.getProviderKey()).in(Scopes.SINGLETON);
        return null;
    }

    @Override
    public Void visit(LinkedKeyBinding<? extends T> binding) {
        builder.to(binding.getLinkedKey()).in(Scopes.SINGLETON);
        return null;
    }

    @Override
    public Void visit(ExposedBinding<? extends T> binding) {
        builder.to(binding.getKey()).in(Scopes.SINGLETON);
        return null;
    }

    @Override
    public Void visit(UntargettedBinding<? extends T> binding) {
        builder.to(binding.getKey().getTypeLiteral()).in(Scopes.SINGLETON);
        return null;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Void visit(ConstructorBinding<? extends T> binding) {
        builder.toConstructor((Constructor<T>) binding.getConstructor().getMember()).in(Scopes.SINGLETON);
        return null;
    }

    @Override
    public Void visit(ConvertedConstantBinding<? extends T> binding) {
        builder.toInstance(binding.getValue());
        return null;
    }

    @Override
    public Void visit(ProviderBinding<? extends T> binding) {
        builder.toProvider(binding.getProvider()).in(Scopes.SINGLETON);
        return null;
    }
}