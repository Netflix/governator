package com.netflix.governator;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;
import com.google.inject.util.Types;
import com.netflix.governator.guice.lazy.FineGrainedLazySingleton;
import com.netflix.governator.guice.lazy.FineGrainedLazySingletonScope;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.netflix.governator.guice.lazy.LazySingletonScope;

/**
 * Adds support for standard lifecycle annotations @PostConstruct and @PreDestroy
 * to Guice.
 * 
 * <pre>
 * {@code
 * public class MyService {
 *    @PostConstruct
 *    public void init() {
 *    }
 *    
 *    @PreDestroy
 *    public void shutdown() {
 *    }
 * }
 * }
 * </pre>
 * 
 * To use simply add LifecycleModule to guice when creating the injector
 * <pre>
 * {@link 
 * Governator.createInjector();
 * }
 * </pre>
 * 
 * See {@link LifecycleInjector} for different scenarios for shutting down the LifecycleManager.
 * 
 * @author elandau
 *
 */
public final class LifecycleModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleModule.class);

    @SuppressWarnings("unchecked")
    private static final TypeLiteral<Set<LifecycleFeature>> FeatureTypeLiteral 
        = (TypeLiteral<Set<LifecycleFeature>>) TypeLiteral.get(Types.setOf(LifecycleFeature.class));
    
    static class TypeLifecycleActions {
        final List<LifecycleAction> postConstructActions = new ArrayList<LifecycleAction>();
        final List<LifecycleAction> preDestroyActions = new ArrayList<>();
    }
    
    static class LifecycleTypeListener extends DefaultLifecycleListener implements TypeListener {
        private final ConcurrentLinkedQueue<Runnable> shutdownActions = new ConcurrentLinkedQueue<Runnable>();
        private final AtomicBoolean isShutdown = new AtomicBoolean();
        
        @Override
        public <I> void hear(final TypeLiteral<I> typeLiteral, final TypeEncounter<I> encounter) {
            final Class<?> clazz = typeLiteral.getRawType();
            final AtomicReference<TypeLifecycleActions> cache = new AtomicReference<TypeLifecycleActions>();
            final Provider<Set<LifecycleFeature>> features = encounter.getProvider(Key.get(FeatureTypeLiteral));
            
            encounter.register(new InjectionListener<I>() {
                @Override
                public void afterInjection(final I injectee) {
                    final TypeLifecycleActions actions = getOrCreateActions();
                    
                    // Call all the LifecycleActions with PostConstruct methods being the last 
                    for (LifecycleAction processor : actions.postConstructActions) {
                        try {
                            processor.call(injectee);
                        } 
                        catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                            throw new ProvisionException("Failed to provision object of type " + clazz.getName(), e);
                        }
                    }
                    
                    // Add any PreDestroy methods to the shutdown list of actions
                    if (!actions.preDestroyActions.isEmpty()) {
                        if (isShutdown.get() == false) {
                            shutdownActions.add(new Runnable() {
                                @Override
                                public void run() {
                                    for (LifecycleAction m : actions.preDestroyActions) {
                                        try {
                                            m.call(injectee);
                                        } 
                                        catch (Exception e) {
                                            LOG.error("Failed to call @PreDestroy method {} on {}", new Object[]{m, injectee.getClass().getName()}, e);
                                        }
                                    }
                                }
                            });
                        }
                        else {
                            LOG.warn("Already shutting down.  Shutdown methods {} on {} will not be invoked", new Object[]{actions.preDestroyActions, injectee.getClass().getName()});
                        }
                    }
                }
                
                public TypeLifecycleActions getOrCreateActions() {
                    TypeLifecycleActions actions = cache.get();
                    if (actions == null) {
                        actions = new TypeLifecycleActions();
                        // Ordered set of actions to perform before PostConstruct 
                        for (LifecycleFeature feature : features.get()) {
                            actions.postConstructActions.addAll(feature.getActionsForType(clazz));
                        }
                        
                        // Finally, add @PostConstruct methods
                        actions.postConstructActions.addAll(PostConstructLifecycleActions.INSTANCE.getActionsForType(clazz));
                        
                        // Determine @PreDestroy methods
                        actions.preDestroyActions.addAll(PreDestroyLifecycleActions.INSTANCE.getActionsForType(clazz));
                        
                        if (!cache.compareAndSet(null, actions)) {
                            actions = cache.get();
                        }
                    }
                    return actions;
                }
            });
        }
        
        /**
         * Invoke all shutdown actions
         */
        @Override
        public synchronized void onStopped() {
            if (isShutdown.compareAndSet(false, true)) {
                for (Runnable action : shutdownActions) {
                    action.run();
                }
            }
        }
        
        @Override
        public synchronized void onStartFailed(Throwable t) {
            onStopped();
        }
    }
    
    @Override
    protected void configure() {
        LifecycleTypeListener listener = new LifecycleTypeListener();
        
        // This is a hack to force early injection into LifecycleTypeListener via static
        // injection which is the first thing injected when Guice is created
        bind(LifecycleTypeListener.class).toInstance(listener);
       
        Multibinder.newSetBinder(binder(), LifecycleListener.class).addBinding().toInstance(listener);
        bindListener(Matchers.any(), listener);
        
        Multibinder.newSetBinder(binder(), LifecycleFeature.class);
        
        // These are essentially obsolete since Guice4 fixes the global lock 
        // and DEVELOPMENT mode makes everything lazy.
        bindScope(FineGrainedLazySingleton.class, FineGrainedLazySingletonScope.get());
        bindScope(LazySingleton.class, LazySingletonScope.get());
   }
}