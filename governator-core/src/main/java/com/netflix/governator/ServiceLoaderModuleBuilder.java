package com.netflix.governator;

import java.util.ServiceLoader;
import java.util.concurrent.Callable;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.ProvisionException;
import com.google.inject.multibindings.Multibinder;

/**
 * Builder for creating a Guice module that either installs modules loaded from the
 * service loader or creates multibindings for a service type.  Method and field @Inject
 * methods of the services will also be invoked.
 * 
 * @author elandau
 *
 * TODO:  Lazy member injection
 */
public class ServiceLoaderModuleBuilder {
    
    private ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
    private Boolean installed = false;

    public ServiceLoaderModuleBuilder usingClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
        return this;
    }
    
    public ServiceLoaderModuleBuilder forInstalledServices(Boolean installed) {
        this.installed = installed;
        return this;
    }

    private abstract class BaseModule<S> extends AbstractModule {
        private final Class<S> type;

        public BaseModule(Class<S> type) {
            this.type = type;
        }
        
        @Override
        protected final void configure() {
            Callable<ServiceLoader<S>> loader;
            if (installed) {
                if (classLoader != null) {
                    throw new RuntimeException("Class loader may not be combined with loading installed services");
                }
                loader = new Callable<ServiceLoader<S>>() {
                    @Override
                    public ServiceLoader<S> call() throws Exception {
                        return ServiceLoader.loadInstalled(type);
                    }
                };
            }
            else if (classLoader != null) {
                loader = new Callable<ServiceLoader<S>>() {
                    @Override
                    public ServiceLoader<S> call() throws Exception {
                        return ServiceLoader.load(type, classLoader);
                    }
                };
            }
            else {
                loader = new Callable<ServiceLoader<S>>() {
                    @Override
                    public ServiceLoader<S> call() throws Exception {
                        return ServiceLoader.load(type);
                    }
                };
            }
            
            try {
                for (S service : loader.call()) {
                    onService(service);
                }
            } catch (Exception e) {
                throw new ProvisionException("Failed to load services for '" + type + "'", e);
            }
        }
        
        protected abstract void onService(S service);
    }
    
    public <S extends Module> Module loadModules(final Class<S> type) {
        return new BaseModule<S>(type) {
            @Override
            protected void onService(S service) {
                install((Module)service);
            }
        };
    }
    
    public <S> Module loadServices(final Class<S> type) {
        return new BaseModule<S>(type) {
            @Override
            protected void onService(S service) {
                requestInjection(service);
                Multibinder.newSetBinder(binder(), type).addBinding().toInstance(service);
            }
        };
    }

}
