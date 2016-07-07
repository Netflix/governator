package com.netflix.governator.internal;

import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Binding;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.spi.BindingScopingVisitor;
import com.netflix.governator.LifecycleAction;
import com.netflix.governator.ManagedInstanceAction;

/**
 * Monitors managed instances and invokes cleanup actions when they
 * become unreferenced
 * 
 * @author tcellucci
 *
 */
public class PreDestroyMonitor implements AutoCloseable {
    private static Logger LOGGER = LoggerFactory.getLogger(PreDestroyMonitor.class);
    
    /**
     * Processes unreferenced markers from the referenceQueue, until
     * the 'running' flag is false or interrupted
     * 
     */
    private final class ScopedCleanupWorker implements Runnable {
        public void run() {
            try {
                while (running.get()) {
                        Reference<? extends ScopeCleanupMarker> ref = markerReferenceQueue.remove(1000);
                        if (ref != null && ref instanceof ScopeCleanupAction) { 
                            UUID markerKey = ((ScopeCleanupAction)ref).getId();
                            ScopeCleanupAction cleanupAction;
                            synchronized(scopedCleanupActions) {
                                cleanupAction = scopedCleanupActions.remove(markerKey);
                            }
                            if (cleanupAction != null) {
                                cleanupAction.call();
                            }
                        }
                }
                LOGGER.info("PreDestroyMonitor.ScopedCleanupWorker is exiting");
            } 
            catch (InterruptedException e) {
                LOGGER.info("PreDestroyMonitor.ScopedCleanupWorker is exiting due to thread interrupt");
            }                
        }
    }

    private static class ScopeCleanupMarker {
        private final UUID id = UUID.randomUUID();
        UUID getId() {
            return id;
        }
    }
    private static final Key<ScopeCleanupMarker> MARKER_KEY = Key.get(ScopeCleanupMarker.class);
    private Deque<Callable<Void>> cleanupActions = new ConcurrentLinkedDeque<>();
    
    private Map<UUID, ScopeCleanupAction> scopedCleanupActions = new LinkedHashMap<>();
    private Map<Class<? extends Annotation>, Scope> scopeBindings;
    private ReferenceQueue<ScopeCleanupMarker> markerReferenceQueue = new ReferenceQueue<>();
    private final ExecutorService reqQueueExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("predestroy-monitor-%d").build());
    private final AtomicBoolean running= new AtomicBoolean(true);
    
    private final Provider<ScopeCleanupMarker> markerProvider = new Provider<ScopeCleanupMarker>() {
        @Override
        public ScopeCleanupMarker get() {
            return new ScopeCleanupMarker();
        }
    };
    
    public PreDestroyMonitor(Map<Class<? extends Annotation>, Scope> scopeBindings) {
        this.scopeBindings = new HashMap<>(scopeBindings);
        this.reqQueueExecutor.submit(new ScopedCleanupWorker());
    }
    
    public <T> boolean register(T destroyableInstance, Binding<T> binding, Iterable<LifecycleAction> action) {
        return (running.get()) ? binding.acceptScopingVisitor(new ManagedInstanceScopingVisitor(destroyableInstance, binding.getSource(), action)) : false;
    }
    
    /*
     * compatibility-mode - scope is assumed to be eager singleton
     */
    public <T> boolean register(T destroyableInstance, Object context, Iterable<LifecycleAction> action) {
        return (running.get()) ? new ManagedInstanceScopingVisitor(destroyableInstance, context, action).visitEagerSingleton() : false;
    }

    /**
     * allows late-binding of scopes to PreDestroyMonitor, useful if more than one Injector contributes scope bindings
     * 
     * @param bindings additional annotation-to-scope bindings to add
     */
    public void addScopeBindings(Map<Class<? extends Annotation>, Scope> bindings) {
        if (running.get()) {
            scopeBindings.putAll(bindings);
        }
    }

    /**
     * final cleanup of managed instances if any
     */
    @Override
    public void close() throws Exception {
        if (running.compareAndSet(true, false)) { // executor thread to exit processing loop
            LOGGER.info("closing PreDestroyMonitor...");
            reqQueueExecutor.shutdown(); // executor to stop 
            synchronized(scopedCleanupActions) { 
                // process any remaining scoped cleanup actions
                for (Callable<Void> actions : scopedCleanupActions.values()) {
                    actions.call();
                }
                scopedCleanupActions.clear();
                scopedCleanupActions = Collections.emptyMap();
            }
            // make sure executor thread really ended
            if (!reqQueueExecutor.awaitTermination(90, TimeUnit.SECONDS)) {
                LOGGER.error("internal executor still active; shutting down now");
                reqQueueExecutor.shutdownNow();
            }
            
            for (Callable<Void> action : cleanupActions) {
                action.call();
            }     
            cleanupActions.clear();
            markerReferenceQueue = null;
            scopeBindings.clear();
            scopeBindings = Collections.emptyMap();
        }
        else {
            LOGGER.warn("PreDestroyMonitor.close() invoked but instance is not running");
        }
    }
    
    /**
     * visits bindingScope of managed instance to set up an appropriate strategy for cleanup,
     * adding actions to either the scopedCleanupActions map or cleanupActions list.  Returns
     * true if cleanup actions were added, false if no cleanup strategy was selected.
     *  
     */
    private final class ManagedInstanceScopingVisitor implements BindingScopingVisitor<Boolean> {       
        private final Object injectee;
        private final Object context;
        private final Iterable<LifecycleAction> lifecycleActions;

        private ManagedInstanceScopingVisitor(Object injectee, Object context, Iterable<LifecycleAction> lifecycleActions) {
            this.injectee = injectee;
            this.context = context;
            this.lifecycleActions = lifecycleActions;
        }

        /*
         * add a hard-reference ManagedInstanceAction to cleanupActions. Cleanup is triggered only at injector shutdown.
         * 
         */
        @Override
        public Boolean visitEagerSingleton() {           
            cleanupActions.addFirst(new ManagedInstanceAction(injectee, lifecycleActions)); // no need for scopedCleanupActions, return the task directly
            return true;
        }

        /*
         * use ScopeCleanupMarker dereferencing strategy to detect scope closure, add new entry to scopedCleanupActions map
         * 
         */
        @Override
        public Boolean visitScope(Scope scope) {
            Provider<ScopeCleanupMarker> scopedMarkerProvider = scope.scope(MARKER_KEY, markerProvider);
            ManagedInstanceAction instanceAction = new ManagedInstanceAction(injectee, lifecycleActions);
            ScopeCleanupMarker marker = scopedMarkerProvider.get();
            UUID markerKey = marker.getId();
            synchronized(scopedCleanupActions) {
                if (scopedCleanupActions.containsKey(markerKey)) {
                    scopedCleanupActions.get(markerKey).add(scopedMarkerProvider, instanceAction);
                }
                else {
                    scopedCleanupActions.put(markerKey, new ScopeCleanupAction(markerKey, scopedMarkerProvider, marker, markerReferenceQueue, instanceAction));
                }
            }
            return true;
        }

        /*
         * lookup Scope by annotation, then delegate to visitScope()
         * 
         */
        @Override
        public Boolean visitScopeAnnotation(final Class<? extends Annotation> scopeAnnotation) {                            
            Scope scope = scopeBindings.get(scopeAnnotation);
            boolean rv;
            if (scope != null) {
                rv = visitScope(scope);
            }
            else {
                LOGGER.warn("no scope binding found for annotation " + scopeAnnotation.getName());
                rv = false;
            }
            return rv;
        }

        /*
         *  add a soft-reference ManagedInstanceAction to cleanupActions deque.  Cleanup triggered only at injector shutdown
         *  if referent has not yet been collected.
         * 
         */
        @Override
        public Boolean visitNoScoping() {
            cleanupActions.addFirst(new ManagedInstanceAction(new SoftReference<Object>(injectee), context, lifecycleActions));  
            return true;
        }
    }

      /**
      * Runnable that weakly references a scopeCleanupMarker and strongly references a list of delegate runnables.  When the marker 
      * is unreferenced, delegates will be invoked in the reverse order of addition.
      */
     private static final class ScopeCleanupAction extends WeakReference<ScopeCleanupMarker> implements Callable<Void> {
         private final UUID id;
         private final List<Callable<Void>> delegates = new ArrayList<>();
         private final List<Provider<ScopeCleanupMarker>> scopeProviders = new ArrayList<>();
         private final AtomicBoolean complete = new AtomicBoolean(false);
         
         public ScopeCleanupAction(UUID id, Provider<ScopeCleanupMarker> scopeProvider, ScopeCleanupMarker marker, ReferenceQueue<ScopeCleanupMarker> refQueue, Callable<Void> delegate) {
             super(marker, refQueue);
             this.id = id;             
             scopeProviders.add(scopeProvider);
             delegates.add(delegate);
         }
         
         public UUID getId() {
             return id;
         }
         
         public void add(Provider<ScopeCleanupMarker> scopeProvider, Callable<Void> action) {
             if (!complete.get()) {
                 delegates.add(0, action);  // add first
                 scopeProviders.add(scopeProvider); // hang onto reference
             }
         }
    
         @Override
         public Void call() {
             if (complete.compareAndSet(false, true)) {
                 for (Callable<Void> r : delegates) {
                     try {
                        r.call();
                    } catch (Exception e) {
                        LOGGER.error("PreDestroy call failed for " + r, e);
                    }
                 }
                 delegates.clear();
                 scopeProviders.clear();
                 clear();
             }
             return null;
         }         
     }    
}
