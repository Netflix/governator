package com.netflix.governator.guice.serviceloader;

import java.util.ServiceLoader;

import com.google.common.collect.Lists;
import com.google.inject.Module;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;

/**
 * BootstrapModule that loads guice modules via the ServiceLoader.
 * 
 * @author elandau
 */
public class ServiceLoaderBootstrapModule implements BootstrapModule {
    private final Class<? extends Module> type;
    
    public ServiceLoaderBootstrapModule() {
        this(Module.class);
    }
    
    public ServiceLoaderBootstrapModule(Class<? extends Module> type) {
        this.type = type;
    }
    
    @Override
    public void configure(BootstrapBinder binder) {
        ServiceLoader<? extends Module> modules = ServiceLoader.load(type);
        binder.includeModules(Lists.newArrayList(modules.iterator()));
    }
}
