package com.netflix.governator.guice.serviceloader;

import java.util.ServiceLoader;

import com.google.inject.Module;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.guice.LifecycleInjectorBuilderSuite;

/**
 * LifecycleInjectorBuilderSuite that loads guice modules via the ServiceLoader.
 * By default the ServiceLoaderSuite will load all Module derived classes.
 * However, more specific module types (i.e. specific subclasses of Module)
 * may be loaded as well.  
 * 
 * @author elandau
 */
public class ServiceLoaderSuite implements LifecycleInjectorBuilderSuite {
    private final Class<? extends Module> type;
    
    public ServiceLoaderSuite() {
        this(Module.class);
    }
    
    public ServiceLoaderSuite(Class<? extends Module> type) {
        this.type = type;
    }
    
    @Override
    public void configure(LifecycleInjectorBuilder builder) {
        builder.withAdditionalModules(ServiceLoader.load(type));
    }
}
