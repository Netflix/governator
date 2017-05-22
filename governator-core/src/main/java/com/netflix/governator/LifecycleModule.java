package com.netflix.governator;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.ProvisionException;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.multibindings.MultibindingsScanner;
import com.google.inject.spi.ProvisionListener;
import com.netflix.governator.annotations.SuppressLifecycleUninitialized;
import com.netflix.governator.internal.BinaryConstant;
import com.netflix.governator.internal.GovernatorFeatureSet;
import com.netflix.governator.internal.JSR250LifecycleAction.ValidationMode;
import com.netflix.governator.internal.PostConstructLifecycleFeature;
import com.netflix.governator.internal.PreDestroyLifecycleFeature;
import com.netflix.governator.internal.PreDestroyMonitor;

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
     */
    static class TypeLifecycleActions {
        final List<LifecycleAction> postConstructActions = new ArrayList<LifecycleAction>();
        final List<LifecycleAction> preDestroyActions = new ArrayList<>();
    }
    
    @Singleton
    @SuppressLifecycleUninitialized
    static class LifecycleProvisionListener extends AbstractLifecycleListener implements ProvisionListener {
        private final ConcurrentMap<Class<?>, TypeLifecycleActions> cache = new ConcurrentHashMap<>(BinaryConstant.I12_4096);
        private Set<LifecycleFeature> features;
        private final AtomicBoolean isShutdown = new AtomicBoolean();
        private PostConstructLifecycleFeature postConstructFeature;
        private PreDestroyLifecycleFeature preDestroyFeature;
        private PreDestroyMonitor preDestroyMonitor;
        private boolean shutdownOnFailure = true;
        

          @SuppressLifecycleUninitialized
        @Singleton
        static class OptionalArgs {
            @com.google.inject.Inject(optional = true)
            GovernatorFeatureSet governatorFeatures;
            
            boolean hasShutdownOnFailure() {
                return governatorFeatures == null ? true : governatorFeatures.get(GovernatorFeatures.SHUTDOWN_ON_ERROR);
            }
            
            ValidationMode getJsr250ValidationMode() {
                return governatorFeatures == null ? ValidationMode.LAX : governatorFeatures.get(GovernatorFeatures.STRICT_JSR250_VALIDATION) ? ValidationMode.STRICT : ValidationMode.LAX;
            }
        }
        @Inject
        public static void initialize(
                final Injector injector,
                OptionalArgs args,
                LifecycleManager manager, 
                LifecycleProvisionListener provisionListener, 
                Set<LifecycleFeature> features) {
            provisionListener.features = features;
            provisionListener.shutdownOnFailure =  args.hasShutdownOnFailure();
            ValidationMode validationMode = args.getJsr250ValidationMode();
            provisionListener.postConstructFeature = new PostConstructLifecycleFeature(validationMode);
            provisionListener.preDestroyFeature = new PreDestroyLifecycleFeature(validationMode);
            provisionListener.preDestroyMonitor = new PreDestroyMonitor(injector.getScopeBindings());
            LOG.debug("LifecycleProvisionListener initialized with features {}", features);
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
                actions.postConstructActions.addAll(postConstructFeature.getActionsForType(type));
                
                // Determine @PreDestroy methods
                actions.preDestroyActions.addAll(preDestroyFeature.getActionsForType(type));
                
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
                    try {
                        preDestroyMonitor.close();
                    } catch (Exception e) {
                        LOG.error("failed closing preDestroyMonitor", e);
                    }
                }
            }
        }
                
        @Override
        public String toString() {
            return "LifecycleProvisionListener@" + System.identityHashCode(this);
        }

        @Override
        public <T> void onProvision(ProvisionInvocation<T> provision) {
            final T injectee = provision.provision();
            if (injectee == null) {
                return;
            }
            if (features == null) {
                if (!injectee.getClass().isAnnotationPresent(SuppressLifecycleUninitialized.class)) {
                    LOG.debug("LifecycleProvisionListener not initialized yet : {}", injectee.getClass());
                }
                  
                // TODO: Add to PreDestroy list
                return;
            }
            
            //Ignore for Spring-managed bindings
            Object source = provision.getBinding().getSource();
            if(source != null && source.toString().contains("spring-guice")) {
                return;
            }
            
            final TypeLifecycleActions actions = getOrCreateActions(injectee.getClass());
            
            // Call all postConstructActions for this injectee
            if (!actions.postConstructActions.isEmpty()) {
                try {
                    new ManagedInstanceAction(injectee, actions.postConstructActions).call();
                } catch (Exception e) {
                    throw new ProvisionException("postConstruct failed", e);
                }
            }
            
            // Add any PreDestroy methods to the shutdown list of actions
            if (!actions.preDestroyActions.isEmpty()) {
                if (isShutdown.get() == false) {
                    preDestroyMonitor.register(injectee, provision.getBinding(), actions.preDestroyActions);
                }
                else {
                    LOG.warn("Already shutting down.  Shutdown methods {} on {} will not be invoked", actions.preDestroyActions, injectee.getClass().getName());
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
        
        install(MultibindingsScanner.asModule());
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
        return "LifecycleModule@" + System.identityHashCode(this);
    }
}
