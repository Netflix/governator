package com.netflix.governator.guice.actions;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.spi.DefaultBindingScopingVisitor;
import com.netflix.governator.guice.PostInjectorAction;

/**
 * Explicit singleton bindings are not eagerly created when running in Stage.DEVELOPMENT.
 * This method iterates through all explicit bindings (though a guice module) for singletons
 * and creates them eagerly after the injector was created.
 */
public class CreateAllBoundSingletons implements PostInjectorAction {

    @Override
    public void call(Injector injector) {
        for (final Binding<?> binding : injector.getBindings().values()) {
            binding.acceptScopingVisitor(new DefaultBindingScopingVisitor<Void>() {
                @Override
                public Void visitScope(Scope scope) {
                    if (scope.equals(Scopes.SINGLETON)) {
                        binding.getProvider().get();
                    }
                    return null;
                }
            });
        }
    }
}
