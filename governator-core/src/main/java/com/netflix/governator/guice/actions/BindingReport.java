package com.netflix.governator.guice.actions;

import java.util.Map;
import java.util.Map.Entry;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.netflix.governator.guice.PostInjectorAction;

public class BindingReport implements PostInjectorAction {
    private static final Logger LOG = LoggerFactory.getLogger(BindingReport.class);
            
    private final String label;
    
    public BindingReport(String label) {
        this.label = label;
    }
    
    public BindingReport() {
        this(">>>> GUICE BINDING REPORT <<<<");
    }
    
    @Override
    public void call(Injector injector) {
        LOG.info("Bindings for " + label);
        for (Entry<Key<?>, Binding<?>> binding : injector.getBindings().entrySet()) {
            LOG.info("Explicit: " + binding.getKey());
        }
        
        Map<Key<?>, Binding<?>> jitBindings = Maps.difference(injector.getAllBindings(), injector.getBindings()).entriesOnlyOnLeft();
        
        for (Entry<Key<?>, Binding<?>> binding : jitBindings.entrySet()) {
            LOG.info("JIT: " + binding.getKey());
        }
    }
}
