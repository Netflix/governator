package com.netflix.governator.guice.serviceloader;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;

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

/**
 * Simple Guice module to integrate with the {@link ServiceLoader}.
 * 
 * @author elandau
 */
public abstract class ServiceLoaderModule extends AbstractModule {

    /**
     * Load services and make them available via a Set<S> binding using 
     * multi-binding.  Note that this methods loads services lazily but also
     * allows for additional bindings to be done via Guice modules.
     * 
     * @param type
     */
    public <S> void loadAndMultibindServices(final Class<S> type) {
        Multibinder<S> binding = Multibinder.newSetBinder(binder(), type);
        for (S service : ServiceLoader.load(type)) {
            binding.addBinding().toProvider(new ServiceProvider<S>(service));
        }
    }

    /**
     * Load services and make them available via a Set<S> binding using 
     * multi-binding.  Note that this methods loads services lazily but also
     * allows for additional bindings to be done via Guice modules.
     * 
     * @param type
     */
    public <S> void loadAndMultibindServices(final Class<S> type, ClassLoader classLoader) {
        Multibinder<S> binding = Multibinder.newSetBinder(binder(), type);
        for (S service : ServiceLoader.load(type, classLoader)) {
            binding.addBinding().toProvider(new ServiceProvider<S>(service));
        }
    }

    /**
     * Load services and make them available via a Set<S> binding using 
     * multi-binding.  Note that this methods loads services lazily but also
     * allows for additional bindings to be done via Guice modules.
     * 
     * @param type
     */
    public <S> void loadAndMultibindInstalledServices(final Class<S> type) {
        Multibinder<S> binding = Multibinder.newSetBinder(binder(), type);
        for (S service : ServiceLoader.loadInstalled(type)) {
            binding.addBinding().toProvider(new ServiceProvider<S>(service));
        }
    }

    /**
     * Create a binding for Set<S> which will be lazily loaded whenever Set<S> is injected.
     * 
     * Note that this method cannot be used with Multibinder.newSetBinder for S.
     * 
     * @param type
     */
    public <S> void lazyLoadAndBindServices(final Class<S> type) {
        TypeLiteral<Set<S>> typeLiteral = (TypeLiteral<Set<S>>) TypeLiteral.get(Types.newParameterizedType(Set.class, type));
        bind(typeLiteral)
            .toProvider(new ServiceSetProvider<S>(new Callable<ServiceLoader<S>>() {
                @Override
                public ServiceLoader<S> call() throws Exception {
                    return ServiceLoader.load(type);
                }
            }))
            .in(Scopes.SINGLETON);
    }
    
    /**
     * Create a binding for Set<S> which will be lazily loaded whenever Set<S> is injected.
     * 
     * Note that this method cannot be used with Multibinder.newSetBinder for S.
     * 
     * @param type
     */
    public <S> void lazyLoadAndBindServices(final Class<S> type, final ClassLoader classLoader) {
        TypeLiteral<Set<S>> typeLiteral = (TypeLiteral<Set<S>>) TypeLiteral.get(Types.newParameterizedType(Set.class, type));
        bind(typeLiteral)
            .toProvider(new ServiceSetProvider<S>(new Callable<ServiceLoader<S>>() {
                @Override
                public ServiceLoader<S> call() throws Exception {
                    return ServiceLoader.load(type, classLoader);
                }
            }))
            .in(Scopes.SINGLETON);
    }
    
    /**
     * Create a binding for Set<S> which will be lazily loaded whenever Set<S> is injected.
     * 
     * Note that this method cannot be used with Multibinder.newSetBinder for S.
     * 
     * @param type
     */
    public <S> void lazyLoadAndBindInstalledServices(final Class<S> type) {
        TypeLiteral<Set<S>> typeLiteral = (TypeLiteral<Set<S>>) TypeLiteral.get(Types.newParameterizedType(Set.class, type));
        bind(typeLiteral)
            .toProvider(new ServiceSetProvider<S>(new Callable<ServiceLoader<S>>() {
                @Override
                public ServiceLoader<S> call() throws Exception {
                    return ServiceLoader.loadInstalled(type);
                }
            }))
            .in(Scopes.SINGLETON);
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
