package com.netflix.governator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.ProvisionException;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.ProvisionListener;
import com.netflix.governator.annotations.SuppressLifecycleUninitialized;
import com.netflix.governator.internal.GovernatorFeatureSet;
import com.netflix.governator.internal.PostConstructLifecycleActions;
import com.netflix.governator.internal.PreDestroyLifecycleActions;
import com.netflix.governator.spi.LifecycleListener;

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
 */
public final class LifecycleModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleModule.class);

    private LifecycleProvisionListener provisionListener = new LifecycleProvisionListener();

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
    @SuppressLifecycleUninitialized
    static class LifecycleProvisionListener extends AbstractLifecycleListener implements ProvisionListener {
        private final ConcurrentLinkedDeque<Runnable> shutdownActions = new ConcurrentLinkedDeque<Runnable>();
        private final ConcurrentMap<Class<?>, TypeLifecycleActions> cache = new ConcurrentHashMap<>();
        private Set<LifecycleFeature> features;
        private final AtomicBoolean isShutdown = new AtomicBoolean();
        private LifecycleManager manager;
        private List<LifecycleListener> pendingLifecycleListeners = new ArrayList<>();
        private boolean shutdownOnFailure = true;
        
        @SuppressLifecycleUninitialized
        @Singleton
        static class OptionalArgs {
            @com.google.inject.Inject(optional = true)
            GovernatorFeatureSet governatorFeatures;
            
            boolean hasShutdownOnFailure() {
                return governatorFeatures == null ? true : governatorFeatures.get(GovernatorFeatures.SHUTDOWN_ON_ERROR);
            }
        }
        @Inject
        public static void initialize(
                OptionalArgs args,
                LifecycleManager manager, 
                LifecycleProvisionListener provisionListener, 
                Set<LifecycleFeature> features) {
            provisionListener.manager = manager;
            provisionListener.features = features;
            provisionListener.shutdownOnFailure =  args.hasShutdownOnFailure();
            
            LOG.debug("LifecycleProvisionListener initialized {}", features);
            
            for (LifecycleListener l : provisionListener.pendingLifecycleListeners) {
                manager.addListener(l);
            }
            provisionListener.pendingLifecycleListeners.clear();
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
        public synchronized void onStopped(Throwable optionalFailureReason) {
            if (shutdownOnFailure || optionalFailureReason == null) {
                if (isShutdown.compareAndSet(false, true)) {
                    for (Runnable action : shutdownActions) {
                        action.run();
                    }
                }
            }
        }
        
        @Override
        public String toString() {
            return "LifecycleProvisionListener[]";
        }

        @Override
        public <T> void onProvision(ProvisionInvocation<T> provision) {
            final T injectee = provision.provision();
            if(injectee == null) {
                return;
            }
            if (features == null) {
                if (!injectee.getClass().isAnnotationPresent(SuppressLifecycleUninitialized.class)) {
                    LOG.debug("LifecycleProvisionListener not initialized yet : {}", injectee.getClass());
                }
                
                if (injectee instanceof LifecycleListener) {
                    pendingLifecycleListeners.add((LifecycleListener)injectee);
                }
                
                // TODO: Add to PreDestroy list
                return;
            }
            
            final TypeLifecycleActions actions = getOrCreateActions(injectee.getClass());
            
            // Call all the LifecycleActions with PostConstruct methods being the last 
            for (LifecycleAction action : actions.postConstructActions) {
                try {
                    action.call(injectee);
                } 
                catch (Exception e) {
                    throw new ProvisionException("Failed to provision object of type " + injectee.getClass(), e);
                }
            }
            
            if (injectee instanceof LifecycleListener) {
                manager.addListener((LifecycleListener)injectee);
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
    }
    
    @Override
    protected void configure() {
        requestStaticInjection(LifecycleProvisionListener.class);
        bind(LifecycleProvisionListener.class).toInstance(provisionListener);
        bindListener(Matchers.any(), provisionListener);
        Multibinder.newSetBinder(binder(), LifecycleFeature.class);
    }
    
    @Override
    public boolean equals(Object obj) {
        return getClass().equals(obj.getClass());
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "LifecycleModule[]";
    }
}