package com.netflix.governator;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.MembersInjector;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.TypeConverterBinding;

/**
 * Wrapper for Guice's Injector with added shutdown methods.  A LifecycleInjector may be created
 * using the utility methods of {@link Governator} which mirror the methods of {@link Guice}
 * but provide shutdown functionality.
 * 
 * <b>Invoking shutdown from outside the injector</b>
 * <pre>
 * {@code 
 *    LifecycleInjector injector = Governator.createInjector();
 *    // ...
 *    injector.shutdown();
 * }
 * 
 * </pre>
 * 
 * <b>Blocking on the injector terminating</b>
 * <pre>
 * {@code 
 *    LifecycleInjector injector = Governator.createInjector();
 *    // ...
 *    injector.awaitTermination();
 * }
 * </pre>
 * 
 * <b>Triggering shutdown from a DI'd class
 * <pre>
 * {@code 
 *    @Singleton
 *    public class SomeShutdownService {
 *        @Inject
 *        SomeShutdownService(LifecycleManager lifecycleManager) {
 *            this.lifecycleManager = lifecycleManager;
 *        }
 *      
 *        void someMethodInvokedForShutdown() {
 *            this.lifecycleManager.shutdown();
 *        }
 *    }
 * }
 * </pre>
 * 
 * <b>Triggering an external event from shutdown without blocking</b>
 * <pre>
 * {@code 
 *    LifecycleInjector injector = Governator.createInjector();
 *    injector.addListener(new LifecycleListener() {
 *        public void onShutdown() {
 *            // Do your shutdown handling here
 *        }
 *    });
 * }
 * </pre>
 * @author elandau
 */
public class LifecycleInjector implements Injector {
    private Injector injector;
    private LifecycleManager manager;
    
    public LifecycleInjector(Injector injector, LifecycleManager manager) {
        this.injector = injector;
        this.manager  = manager;
    }
    
    /**
     * Block until LifecycleManager terminates
     * 
     * @param injector
     * @throws InterruptedException
     */
    public void awaitTermination() throws InterruptedException {
        manager.awaitTermination();
    }

    /**
     * Shutdown LifecycleManager on this Injector which will invoke all registered
     * {@link LifecycleListener}s and unblock awaitTermination. 
     * 
     * @param injector
     */
    public void shutdown() {
        manager.shutdown();
    }
    
    /**
     * Register a single shutdown listener for async notification of the LifecycleManager
     * terminating. 
     * 
     * @param injector
     * @param listener
     */
    public void addListener(LifecycleListener listener) {
        manager.addListener(listener);
    }

    @Override
    public void injectMembers(Object instance) {
        injector.injectMembers(instance);
    }

    @Override
    public <T> MembersInjector<T> getMembersInjector(TypeLiteral<T> typeLiteral) {
        return injector.getMembersInjector(typeLiteral);
    }

    @Override
    public <T> MembersInjector<T> getMembersInjector(Class<T> type) {
        return injector.getMembersInjector(type);
    }

    @Override
    public Map<Key<?>, Binding<?>> getBindings() {
        return injector.getBindings();
    }

    @Override
    public Map<Key<?>, Binding<?>> getAllBindings() {
        return injector.getAllBindings();
    }

    @Override
    public <T> Binding<T> getBinding(Key<T> key) {
        return injector.getBinding(key);
    }

    @Override
    public <T> Binding<T> getBinding(Class<T> type) {
        return injector.getBinding(type);
    }

    @Override
    public <T> Binding<T> getExistingBinding(Key<T> key) {
        return injector.getExistingBinding(key);
    }

    @Override
    public <T> List<Binding<T>> findBindingsByType(TypeLiteral<T> type) {
        return injector.findBindingsByType(type);
    }

    @Override
    public <T> Provider<T> getProvider(Key<T> key) {
        return injector.getProvider(key);
    }

    @Override
    public <T> Provider<T> getProvider(Class<T> type) {
        return injector.getProvider(type);
    }

    @Override
    public <T> T getInstance(Key<T> key) {
        return injector.getInstance(key);
    }

    @Override
    public <T> T getInstance(Class<T> type) {
        return injector.getInstance(type);
    }

    @Override
    public Injector getParent() {
        return injector.getParent();
    }

    @Override
    public Injector createChildInjector(Iterable<? extends Module> modules) {
        return injector.createChildInjector(modules);
    }

    @Override
    public Injector createChildInjector(Module... modules) {
        return injector.createChildInjector(modules);
    }

    @Override
    public Map<Class<? extends Annotation>, Scope> getScopeBindings() {
        return injector.getScopeBindings();
    }

    @Override
    public Set<TypeConverterBinding> getTypeConverterBindings() {
        return injector.getTypeConverterBindings();
    }
}
