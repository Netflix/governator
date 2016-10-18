package com.netflix.governator.visitors;

import com.google.inject.Binding;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.InstanceBinding;

/*
 * Specialized {@link DefaultElementVisitor} that formats a warning for any toInstance() binding.  
 * Use this with {@link InjectorBuilder#forEachElement} to identify situations where toInstance() bindings 
 * are used.  The assertion here is that toInstance bindings are an indicator of provisioning being
 * done outside of Guice and could involve static initialization that violates ordering guarantees
 * of a DI framework.  The recommendation is to replace toInstance() bindings with @Provides methods.
 */
public final class WarnOfToInstanceInjectionVisitor extends DefaultElementVisitor<String> { 
    public <T> String visit(Binding<T> binding) {
        return binding.acceptTargetVisitor(new DefaultBindingTargetVisitor<T, String>() {
            public String visit(InstanceBinding<? extends T> instanceBinding) {
                return String.format("toInstance() at %s can force undesireable static initialization.  " +
                        "Consider replacing with an @Provides method instead.",
                        instanceBinding.getSource());
            }
        });
    }
}
