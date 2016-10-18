package com.netflix.governator.visitors;

import com.google.inject.Binding;
import com.google.inject.spi.DefaultBindingTargetVisitor;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.InstanceBinding;

public class WarnOfToInstanceInjectionVisitor extends DefaultElementVisitor<String> { 
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
