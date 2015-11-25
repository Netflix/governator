package com.netflix.governator;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Key;
import com.google.inject.ProvisionException;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.ProvisionListener;
import com.netflix.governator.guice.lazy.FineGrainedLazySingleton;
import com.netflix.governator.guice.lazy.FineGrainedLazySingletonScope;
import com.netflix.governator.guice.lazy.LazySingleton;
import com.netflix.governator.guice.lazy.LazySingletonScope;

/**
 * Adds support for standard lifecycle annotations @PostConstruct and @PreDestroy to Guice.
 * 
 * <code>
 * public class MyService {
 *    {@literal @}PostConstruct
 *    public void init() {
 *    }
 *    
 *    {@literal @}PreDestroy
 *    public void shutdown() {
 *    }
 * }
 * </code>
 * 
 * To use simply add LifecycleModule to guice when creating the injector
 * 
 * See {@link LifecycleInjector} for different scenarios for shutting down the LifecycleManager.
 * 
 * @deprecated Moved to karyon
 */
@Deprecated
public final class LifecycleModule extends SingletonModule {
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleModule.class);

    /**
     * Holder of actions for a specific type.
     * 
     * @author elandau
     */
    static class TypeLifecycleActions {
        final List<LifecycleAction> postConstructActions = new ArrayList<LifecycleAction>();
        final List<LifecycleAction> preDestroyActions = new ArrayList<>();
    }
    
    @Singleton
    static class LifecycleProvisionListener extends DefaultLifecycleListener implements ProvisionListener {
        private final ConcurrentLinkedDeque<Runnable> shutdownActions = new ConcurrentLinkedDeque<Runnable>();
        private final ConcurrentMap<Class<?>, TypeLifecycleActions> cache = new ConcurrentHashMap<>();
        private Set<LifecycleFeature> features;
        private final AtomicBoolean isShutdown = new AtomicBoolean();
        private ProvisionMetrics metrics;
        private LifecycleManager manager;
        private ConcurrentLinkedQueue<LifecycleListener> pendingLifecycleListeners = new ConcurrentLinkedQueue<>();
        private boolean shutdownOnFailure = true;
        
        private static class Optional {
            @com.google.inject.Inject(optional=true)
            GovernatorConfiguration config;
        }
        
        @Inject
        public static void initialize(
                LifecycleManager manager, 
                LifecycleProvisionListener listener, 
                Set<LifecycleFeature> features, 
                ProvisionMetrics metrics, 
                Optional optional) {
            LOG.debug("LifecycleProvisionListener initialized {}", features);
            listener.metrics = metrics;
            listener.manager = manager;
            listener.manager.addListener(listener);
            listener.features = features;
            listener.shutdownOnFailure = optional.config == null 
                    ? true 
                    : optional.config.isEnabled(GovernatorFeatures.SHUTDOWN_ON_ERROR);
            
            LifecycleListener l;
            while (null != (l = listener.pendingLifecycleListeners.poll())) {
                manager.addListener(l);
            }
        }
        
        @Override
        public <T> void onProvision(ProvisionInvocation<T> provision) {
            final Key<?> key = provision.getBinding().getKey();
            final Class<?> clazz = key.getTypeLiteral().getRawType();
            
            final T injectee;
            if (features == null) {
                LOG.debug("LifecycleProvisionListener not initialized yet : {} source={}", key, provision.getBinding().getSource());

                injectee = provision.provision();
                
                if (injectee instanceof LifecycleListener) {
                    pendingLifecycleListeners.add((LifecycleListener)injectee);
                }
                
                // TODO: Add to PreDestroy list
                return;
            }
            
            final TypeLifecycleActions actions = getOrCreateActions(clazz);
            
            // Instantiate the type and pass to the metrics.  This time captured will
            // include invoking any lifecycle events.
            metrics.push(key);
            try {
                injectee = provision.provision();
            
                // Call all the LifecycleActions with PostConstruct methods being the last 
                for (LifecycleAction processor : actions.postConstructActions) {
                    try {
                        processor.call(injectee);
                    } 
                    catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                        throw new ProvisionException("Failed to provision object of type " + key, e);
                    }
                }
                
                if (injectee instanceof LifecycleListener) {
                    manager.addListener((LifecycleListener)injectee);
                }
            }
            finally {
                metrics.pop();
            }
            
            // Add any PreDestroy methods to the shutdown list of actions
            if (!actions.preDestroyActions.isEmpty()) {
                if (isShutdown.get() == false) {
                    shutdownActions.addFirst(new Runnable() {
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
        
        public TypeLifecycleActions getOrCreateActions(Class<?> type) {
            TypeLifecycleActions actions = cache.get(type);
            if (actions == null) {
                actions = new TypeLifecycleActions();
                // Ordered set of actions to perform before PostConstruct 
                for (LifecycleFeature feature : features) {
                    actions.postConstructActions.addAll(feature.getActionsForType(type));
                }
                
                // Finally, add @PostConstruct methods
                actions.postConstructActions.addAll(PostConstructLifecycleActions.INSTANCE.getActionsForType(type));
                
                // Determine @PreDestroy methods
                actions.preDestroyActions.addAll(PreDestroyLifecycleActions.INSTANCE.getActionsForType(type));
                
                TypeLifecycleActions existing = cache.putIfAbsent(type, actions);
                if (existing != null) {
                    return existing;
                }
            }
            return actions;
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
            if (shutdownOnFailure) {
                onStopped();
            }
        }
    }
    
    @Override
    protected void configure() {
        LifecycleProvisionListener listener = new LifecycleProvisionListener();
        requestStaticInjection(LifecycleProvisionListener.class);
        bind(LifecycleProvisionListener.class).toInstance(listener);
        bindListener(Matchers.any(), listener);
        Multibinder.newSetBinder(binder(), LifecycleFeature.class);
        
        // These are essentially obsolete since Guice4 fixes the global lock 
        // and DEVELOPMENT mode makes everything lazy.
        bindScope(FineGrainedLazySingleton.class, FineGrainedLazySingletonScope.get());
        bindScope(LazySingleton.class, LazySingletonScope.get());
   }
}