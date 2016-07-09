package com.netflix.governator.internal;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.Binding;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.spi.BindingScopingVisitor;
import com.google.inject.util.Providers;
import com.netflix.governator.LifecycleAction;
import com.netflix.governator.ManagedInstanceAction;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.annotation.Annotation;
import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.SoftReference;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    
    private ConcurrentMap<UUID, ScopeCleanupAction> scopedCleanupActions = new ConcurrentHashMap<>(1<<14);
    private Map<Class<? extends Annotation>, Scope> scopeBindings;
    private ReferenceQueue<ScopeCleanupMarker> markerReferenceQueue = new ReferenceQueue<>();
    private final ExecutorService reqQueueExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("predestroy-monitor-%d").build());
    private final AtomicBoolean running= new AtomicBoolean(true);
    
    final static class ScopeCleanupMarkerProvider implements Provider<ScopeCleanupMarker> {
        final static ScopeCleanupMarkerProvider instance = new ScopeCleanupMarkerProvider();
        @Override
        public ScopeCleanupMarker get() {
            return new ScopeCleanupMarker();
        }
    }
    
    private final ScopeCleanupMarker singletonMarker = new ScopeCleanupMarker();
    
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
                List<ScopeCleanupAction> values = new ArrayList<>(scopedCleanupActions.values());
                Collections.sort(values);
                for (Callable<Void> actions : values) {
                    actions.call();
                }
                scopedCleanupActions.clear();
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
            // Special case Singleton scopes since a thread local for singleton scopes will not 
            // have been properly initialized if being created lazily from another scope
            // as part of another scope (such as RequestScope).
            final Provider<ScopeCleanupMarker> scopedMarkerProvider;
            if (scope.equals(Scopes.SINGLETON) || (scope instanceof AbstractScope && ((AbstractScope)scope).isSingletonScope())) {
                scopedMarkerProvider = Providers.of(singletonMarker);
            } else {
                scopedMarkerProvider = scope.scope(MARKER_KEY, ScopeCleanupMarkerProvider.instance);                
            }
                    
            ScopeCleanupMarker marker = scopedMarkerProvider.get();                
            UUID markerKey = marker.getId();
            synchronized (markerKey) {
                ManagedInstanceAction instanceAction = new ManagedInstanceAction(injectee, lifecycleActions);
                ScopeCleanupAction newSca = new ScopeCleanupAction(markerKey, marker, markerReferenceQueue);
                if (scopedCleanupActions.putIfAbsent(markerKey, newSca) == null) {
                    newSca.add(scopedMarkerProvider, instanceAction);
                }
                else {
                    scopedCleanupActions.get(markerKey).add(scopedMarkerProvider, instanceAction);
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
     private static final class ScopeCleanupAction extends WeakReference<ScopeCleanupMarker> implements Callable<Void>, Comparable<ScopeCleanupAction> {
         private volatile static long instanceCounter = 0;
         private final UUID id;
         private final long ordinal;
         private List<Callable<Void>> delegates;
         private Set<Provider<ScopeCleanupMarker>> scopeProviders ;
         private final AtomicBoolean complete = new AtomicBoolean(false);
         
         public ScopeCleanupAction(UUID id, ScopeCleanupMarker marker, ReferenceQueue<ScopeCleanupMarker> refQueue) {
             super(marker, refQueue);
             this.id = id;            
             this.ordinal = instanceCounter++;
         }
         
         public UUID getId() {
             return id;
         }
         
         public synchronized void add(Provider<ScopeCleanupMarker> scopeProvider, Callable<Void> action) {
             if (!complete.get()) {
                 if (delegates == null) {
                     delegates = new ArrayList<>();
                     scopeProviders = new HashSet<>();
                 }
                 delegates.add(0, action);  // add first
                 scopeProviders.add(scopeProvider); // hang onto reference
             }
         }
    
         @Override
         public synchronized Void call() {
             if (complete.compareAndSet(false, true) && delegates != null) {
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

        @Override
        public int compareTo(ScopeCleanupAction o) {
            return Long.compare(ordinal, o.ordinal);
        }         
     }    
}
