package com.netflix.governator;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Module;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.netflix.governator.spi.ModuleListTransformer;

public class BindingLoggingModuleTransformer implements ModuleListTransformer {
    private static final Logger LOG = LoggerFactory.getLogger(BindingLoggingModuleTransformer.class);
    
    @Override
    public List<Module> transform(List<Module> modules) {
        for (Element binding : Elements.getElements(modules)) {
            LOG.debug("Binding : {}", binding);
        }
        
        return modules;
    }
}
