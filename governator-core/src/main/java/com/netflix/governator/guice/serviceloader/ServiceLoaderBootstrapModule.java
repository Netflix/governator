package com.netflix.governator.guice.serviceloader;

import java.util.ServiceLoader;

import com.google.common.collect.Lists;
import com.google.inject.Module;
import com.netflix.governator.guice.AbstractBootstrapModule;

/**
 * BootstrapModule that loads guice modules via the ServiceLoader.
 * 
 * @author elandau
 */
public class ServiceLoaderBootstrapModule extends AbstractBootstrapModule {
    private final Class<? extends Module> type;
    
    public ServiceLoaderBootstrapModule() {
        this(Module.class);
    }
    
    public ServiceLoaderBootstrapModule(Class<? extends Module> type) {
        this.type = type;
    }
    
    @Override
    public void configure() {
        ServiceLoader<? extends Module> modules = ServiceLoader.load(type);
        includeModules(Lists.newArrayList(modules.iterator()));
    }
}
