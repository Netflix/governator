package com.netflix.governator.guice.serviceloader;

import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.google.inject.Scopes;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderWithExtensionVisitor;
import com.google.inject.spi.Toolable;
import com.google.inject.util.Types;
import com.netflix.governator.guice.lazy.LazySingletonScope;

/**
 * Simple Guice module to integrate with the {@link ServiceLoader}.
 * 
 * @author elandau
 */
public abstract class ServiceLoaderModule extends AbstractModule {

    public interface ServiceBinder<S> {
        public ServiceBinder<S> usingClassLoader(ClassLoader classLoader);
        public ServiceBinder<S> forInstalledServices(Boolean installed);
        public ServiceBinder<S> usingMultibinding(Boolean usingMultibinding);
    }
    
    static class ServiceBinderImpl<S> extends AbstractModule implements ServiceBinder<S> {
        private final Class<S> type;
        private ClassLoader classLoader;
        private boolean installed = false;
        private boolean asMultibinding = false;
        
        ServiceBinderImpl(Class<S> type) {
            this.type = type;
        }
        
        @Override
        public ServiceBinder<S> usingClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }
        
        @Override
        public ServiceBinder<S> forInstalledServices(Boolean installed) {
            this.installed = installed;
            return this;
        }
        
        @Override
        public ServiceBinder<S> usingMultibinding(Boolean usingMultibinding) {
            this.asMultibinding = usingMultibinding;
            return this;
        }
        
        protected void configure() {
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
            
            if (asMultibinding) {
                Multibinder<S> binding = Multibinder.newSetBinder(binder(), type);
                ServiceLoader<S> services;
                try {
                    for (S service : loader.call()) {
                        System.out.println("Adding binding for service : " + service.getClass().getName());
                        ServiceProvider<S> provider = new ServiceProvider<S>(service);
                        binding.addBinding().toProvider(provider).in(Scopes.SINGLETON);
                    }
                } catch (Exception e) {
                    throw new ProvisionException("Failed to load services for '" + type + "'", e);
                }
            }
            else {
                @SuppressWarnings("unchecked")
                TypeLiteral<Set<S>> typeLiteral = (TypeLiteral<Set<S>>) TypeLiteral.get(Types.setOf(type));
                bind(typeLiteral)
                    .toProvider(new ServiceSetProvider<S>(loader))
                    .in(LazySingletonScope.get());
            }
        }
    }
    
    private final List<ServiceBinderImpl<?>> binders = Lists.newArrayList();
    
    @Override
    public final void configure() {
        configureServices();
        
        for (ServiceBinderImpl<?> binder : binders) {
            install(binder);
        }
    }
    
    protected abstract void configureServices();
    
    /**
     * Load services and make them available via a Set<S> binding using 
     * multi-binding.  Note that this methods loads services lazily but also
     * allows for additional bindings to be done via Guice modules.
     * 
     * @param type
     */
    public <S> ServiceBinder<S> bindServices(final Class<S> type) {
        ServiceBinderImpl<S> binder = new ServiceBinderImpl<S>(type);
        binders.add(binder);
        return binder;
    }
    
    /**
     * Custom provider that enables member injection on services
     * 
     * @author elandau
     *
     * @param <S>
     */
    public static class ServiceSetProvider<S> implements ProviderWithExtensionVisitor<Set<S>> {
        
        private Injector injector;
        private Callable<ServiceLoader<S>> loader;
        
        public ServiceSetProvider(Callable<ServiceLoader<S>> loader) {
            this.loader = loader;
        }
        
        @Override
        public Set<S> get() {
            Set<S> services = Sets.newHashSet();
            try {
                for (S obj : loader.call()) {
                    injector.injectMembers(obj);
                    services.add(obj);
                }
            } catch (Exception e) {
                throw new ProvisionException("Failed to laod services", e);
            } 
            return services;
        }

        @Override
        public <B, V> V acceptExtensionVisitor(
                BindingTargetVisitor<B, V> visitor,
                ProviderInstanceBinding<? extends B> binding) {
            return visitor.visit(binding);
        }

        @Inject
        @Toolable
        void initialize(Injector injector) {
            this.injector = injector;
        }
    }
    
    /**
     * Custom provider that allows for member injection of a service.  Note that while the
     * service was instantiated at binding time the members injection won't happen until the
     * set of services is injected.
     * 
     * @author elandau
     *
     * @param <S>
     */
    public static class ServiceProvider<S> implements ProviderWithExtensionVisitor<S> {
        private S service;
        
        public ServiceProvider(S service) {
            this.service = service;
        }
        
        @Override
        public S get() {
            System.out.println("Get : " + service.getClass().getName());
            return service;
        }

        @Override
        public <B, V> V acceptExtensionVisitor(
                BindingTargetVisitor<B, V> visitor,
                ProviderInstanceBinding<? extends B> binding) {
            return visitor.visit(binding);
        }

        @Inject
        @Toolable
        void initialize(Injector injector) {
            injector.injectMembers(service);
        }
    }

}
