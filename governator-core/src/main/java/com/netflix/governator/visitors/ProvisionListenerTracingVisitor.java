package com.netflix.governator.visitors;

import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.ProvisionListenerBinding;

public class ProvisionListenerTracingVisitor extends DefaultElementVisitor<String> { 
    public String visit(ProvisionListenerBinding binding) {
        return String.format("Provision listener %s matching %s at %s",  
            binding.getListeners(), binding.getBindingMatcher(), binding.getSource()); 
    }
}
