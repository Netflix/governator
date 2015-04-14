package com.netflix.governator.guice.concurrent;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.Dependency;
import com.google.inject.spi.InjectionPoint;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderWithExtensionVisitor;
import com.google.inject.spi.Toolable;
import com.netflix.governator.annotations.NonConcurrent;
import com.netflix.governator.lifecycle.LifecycleListener;

/**
 * Utility class for creating Providers that allow for concurrent instantiation
 * of dependencies to a type.
 * 
 * @author elandau
 *
 */
public class ConcurrentProviders {
    /**
     * Create a Provider that will construct all constructor arguments in parallel and wait
     * for all dependencies to be constructed before invoking the constructor of the type.
     * 
     * For example, consider the following class that has 4 dependencies
     * 
     * {@code 
     * @Singleton
     * public class Foo {
     *     @Inject
     *     public Foo(@NonConcurrent NonConcurrentSingleton, DependencyA a, DependencyB b, Provider<DependencyC> c, NonSingletonD d) {
     *     }
     * }
     * }
     * 
     * and the following Guice binding to enable the concurrent behavior,
     * 
     * {@code
     * public configure() {
     *     bind(Foo.class).toProvider(ConcurrentProviders.of(Foo.class)).asEagerSingleton();
     * }
     * }
     * 
     * When Foo is created eagerly (by Guice) the provider will spawn 4 threads each creating
     * one of the above dependencies.  Note that for Provider<DependencyC> the provider will 
     * be created and not an instance of DependencyC.  Also, note that NonConcurrentSingleton
     * will not be constructed in a separate thread.
     * 
     * Note that a dedicated pool of N threads (where N is the number of dependencies) is created
     * when Foo is first constructed.  Upon instantiation of Foo the pool is shut down and the 
     * resulting instance of Foo cached for future retrieval.  
     * 
     * It's also important to note that ALL transitive dependencies of Foo MUST be in the
     * <b>FineGrainedLazySingleton</b> scope, otherwise there is a high risk of hitting the global Guice
     * Singleton scope deadlock issue.  Any parameter that causes this deadlock can be annotated 
     * with @NonConcurrent to force it to be created within the same thread as the injectee.
     * 
     * @param type
     * @return
     */
    public static <T> Provider<T> of(final Class<? extends T> type) {
        return new ProviderWithExtensionVisitor<T>() {
            private volatile T instance;
            private Injector injector;
            private Set<LifecycleListener> listeners = Collections.emptySet();
            
            public T get() {
                if ( instance == null ) {
                    synchronized (this) {
                        if ( instance == null ) {
                            instance = createAndInjectMember();
                        }
                    }
                }
                return instance;
            }
            
            private T createAndInjectMember() {
                T instance = create();
                injector.injectMembers(instance);
                return instance;
            }
            
            private T create() {
                // Look for an @Inject constructor or just create a new instance if not found
                InjectionPoint injectionPoint = InjectionPoint.forConstructorOf(type);
                final long startTime = System.nanoTime();
                
                for (LifecycleListener listener : listeners) {
                    listener.objectInjecting(TypeLiteral.get(type));
                }
                if (injectionPoint != null) {
                    List<Dependency<?>> deps = injectionPoint.getDependencies();
                    if (deps.size() > 0) {
                        Constructor<?> constructor = (Constructor<?>)injectionPoint.getMember();
                        // One thread for each dependency
                        ExecutorService executor = Executors.newCachedThreadPool(
                                new ThreadFactoryBuilder()
                                    .setDaemon(true)
                                    .setNameFormat("ConcurrentProviders-" + type.getSimpleName() + "-%d")
                                    .build());
                        try {
                            List<Supplier<?>> suppliers = Lists.newArrayListWithCapacity(deps.size());
                            
                            // Iterate all constructor dependencies and get and instance from the Injector
                            for (final Dependency<?> dep : deps) {
                                if (!isConcurrent(constructor, dep.getParameterIndex())) {
                                    suppliers.add(getCreator(dep.getKey()));
                                }
                                else {
                                    final Future<?> future = executor.submit(new Callable<Object>() {
                                        @Override
                                        public Object call() throws Exception {
                                            return getCreator(dep.getKey()).get();
                                        }
                                    });
                                    suppliers.add(new Supplier() {
                                        @Override
                                        public Object get() {
                                            try {
                                                return future.get();
                                            } catch (InterruptedException e) {
                                                Thread.currentThread().interrupt();
                                                throw new ProvisionException("interrupted during provision");
                                            } catch (ExecutionException e) {
                                                throw new RuntimeException(e.getCause());
                                            }
                                        }
                                    });
                                }
                            }
                            // All dependencies are now being instantiated in parallel
                            
                            // Fetch the arguments from the futures and put in an array to pass to newInstance
                            List<Object> params = Lists.newArrayListWithCapacity(deps.size());
                            for (Supplier<?> supplier: suppliers) {
                                params.add(supplier.get());
                            }
                            
                            // All dependencies have been initialized
                            
                            // Look for the @Inject constructor and invoke it.
                            try {
                                T obj = (T)constructor.newInstance(params.toArray());
                                long duration = System.nanoTime() - startTime;
                                for (LifecycleListener listener : listeners) {
                                    listener.objectInjected((TypeLiteral<T>)TypeLiteral.get(type), obj, duration, TimeUnit.NANOSECONDS);
                                }
                                return obj;
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        } 
                        finally {
                            executor.shutdown();
                        }
                    }
                }
                
                try {
                    T obj = type.newInstance();
                    long duration = System.nanoTime() - startTime;
                    for (LifecycleListener listener : listeners) {
                        listener.objectInjected((TypeLiteral<T>)TypeLiteral.get(type), obj, duration, TimeUnit.NANOSECONDS);
                    }
                    return obj;
                } catch (Exception e) {
                    e.printStackTrace();
                    throw new ProvisionException("Error constructing object of type " + type.getName(), e);
                }
            }
            
            private boolean isConcurrent(Constructor<?> constructor, int parameterIndex) {
                Annotation[] annots = constructor.getParameterAnnotations()[parameterIndex];
                if (annots != null) {
                    for (Annotation annot : annots) {
                        if (annot.annotationType().equals(NonConcurrent.class)) {
                            return false;
                        }
                    }
                }
                return true;
            }

            /**
             * Required to get the Injector in {@link initialize()}
             */
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
            
            @Inject(optional = true) 
            void setListeners(Set<LifecycleListener> listeners) {
                this.listeners = listeners;
            }
            
            public <S> Supplier<S> getCreator(final Key<S> key) {
                return new Supplier<S>() {
                    @Override
                    public S get() {
                        final long startTime = System.nanoTime();
                        for (LifecycleListener listener : listeners) {
                            listener.objectInjecting(key.getTypeLiteral());
                        }
                        S obj = injector.getInstance(key);
                        final long duration = System.nanoTime() - startTime;
                        for (LifecycleListener listener : listeners) {
                            listener.objectInjected(key.getTypeLiteral(), obj, duration, TimeUnit.NANOSECONDS);
                        }
                        return obj;
                    }
                };
            }

        };
    }
}
