package com.netflix.governator.guice.actions;

import java.lang.annotation.Annotation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.spi.DefaultBindingScopingVisitor;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.LinkedKeyBinding;
import com.netflix.governator.guice.PostInjectorAction;

/**
 * Explicit singleton bindings are not eagerly created when running in Stage.DEVELOPMENT.
 * This method iterates through all explicit bindings (those made though a guice module) for singletons
 * and creates them eagerly after the injector was created.
 */
public class CreateAllBoundSingletons implements PostInjectorAction {
    @Override
    public void call(Injector injector) {
        for (final Binding<?> binding : injector.getBindings().values()) {
            binding.acceptVisitor(new DefaultElementVisitor<Void>() {
                public <T> Void visit(final Binding<T> binding) {
                    // This takes care of bindings to concrete classes
                    binding.acceptScopingVisitor(new DefaultBindingScopingVisitor<Void>() {
                        @Override
                        public Void visitScope(Scope scope) {
                            if (scope.equals(Scopes.SINGLETON)) {
                                binding.getProvider().get();
                            }
                            return null;
                        }
                    });
                    
                    // This takes care of the interface .to() bindings
                    binding.acceptTargetVisitor(new DefaultBindingTargetVisitor<T, Void>() {
                        public Void visit(LinkedKeyBinding<? extends T> linkedKeyBinding) {
                            Key<?> key = linkedKeyBinding.getLinkedKey();
                            Class<?> type = key.getTypeLiteral().getRawType();
                            if (type.getAnnotation(Singleton.class) != null ||
                                type.getAnnotation(javax.inject.Singleton.class) != null) {
                                    binding.getProvider().get();
                            }
                            return null;
                        }
                        
                    });
                    return visitOther(binding);
                }
            });
        }
    }
}
