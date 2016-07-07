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
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

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
    
     
    private static class ScopeCleanupMarker {
    }
    
    private Deque<Callable<Void>> cleanupActions = new ConcurrentLinkedDeque<>();
    
    private Map<Integer, ScopeCleanupAction> scopedCleanupActions = new LinkedHashMap<>();
    private Map<Class<? extends Annotation>, Scope> scopeBindings;
    private ReferenceQueue<ScopeCleanupMarker> markerReferenceQueue = new ReferenceQueue<>();
    private final ExecutorService reqQueueExecutor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean running;
    
    private final Provider<ScopeCleanupMarker> markerProvider = new Provider<ScopeCleanupMarker>() {
        @Override
        public ScopeCleanupMarker get() {
            return new ScopeCleanupMarker();
        }
    };
    
    public PreDestroyMonitor(Map<Class<? extends Annotation>, Scope> scopeBindings) {
        this.scopeBindings = new HashMap<>(scopeBindings);
        running = new AtomicBoolean(true);
        reqQueueExecutor.submit(new Runnable() {
            public void run() {
                try {
                    while (running.get()) {
                            Reference<? extends ScopeCleanupMarker> ref = markerReferenceQueue.remove(1000);
                            if (ref != null && ref instanceof ScopeCleanupAction) { 
                                Integer markerKey = ((ScopeCleanupAction)ref).getId();
                                ScopeCleanupAction cleanupAction;
                                synchronized(scopedCleanupActions) {
                                    cleanupAction = scopedCleanupActions.remove(markerKey);
                                }
                                if (cleanupAction != null) {
                                    cleanupAction.call();
                                }
                            }
                    }
                } 
                catch (InterruptedException e) {
                }                
            }
        });
    }
    
    public <T> boolean register(T destroyableInstance, Binding<T> binding, Iterable<LifecycleAction> action) {
        return binding.acceptScopingVisitor(new ManagedInstanceScopingVisitor(destroyableInstance, binding.getSource(), action));
    }
    
    public void addScopeBindings(Map<Class<? extends Annotation>, Scope> bindings) {
        scopeBindings.putAll(bindings);
    }

    /*
     * compatibility-mode - scope is assumed to be eager singleton
     */
    public <T> boolean register(T destroyableInstance, Object context, Iterable<LifecycleAction> action) {
        return new ManagedInstanceScopingVisitor(destroyableInstance, context, action).visitEagerSingleton();
    }
    
    /**
     * final cleanup of managed instances if any
     */
    @Override
    public synchronized void close() throws Exception {
        if (running.compareAndSet(true, false)) {
            reqQueueExecutor.shutdown();
            reqQueueExecutor.awaitTermination(2, TimeUnit.SECONDS);
            if (!reqQueueExecutor.isTerminated()) reqQueueExecutor.shutdownNow();
            synchronized(scopedCleanupActions) {
                for (Callable<Void> actions : scopedCleanupActions.values()) {
                    actions.call();
                }
                scopedCleanupActions.clear();
                scopedCleanupActions = Collections.emptyMap();
            }
            
            
            
            for (Callable<Void> action : cleanupActions) {
                action.call();
            }     
            cleanupActions.clear();
            markerReferenceQueue = null;
            scopeBindings.clear();
            scopeBindings = Collections.emptyMap();
        }
    }
    
    /**
     * visits bindingScope of managed instance to set up an appropriate strategy for cleanup
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
            Provider<ScopeCleanupMarker> scopedMarkerProvider = scope.scope(Key.get(ScopeCleanupMarker.class), markerProvider);
            ManagedInstanceAction instanceAction = new ManagedInstanceAction(injectee, lifecycleActions);
            ScopeCleanupMarker marker = scopedMarkerProvider.get();
            Integer markerKey = System.identityHashCode(marker);
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
            return (scope != null) ? visitScope(scope) : false;
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
      * is dereferenced, delegates will be invoked in the reverse order of addition.
      */
     private static final class ScopeCleanupAction extends WeakReference<ScopeCleanupMarker> implements Callable<Void> {
         private final Integer id;
         private final List<Callable<Void>> delegates = new ArrayList<>();
         private final List<Provider<ScopeCleanupMarker>> scopeProviders = new ArrayList<>();
         private volatile boolean complete = false;
         public ScopeCleanupAction(Integer id, Provider<ScopeCleanupMarker> scopeProvider, ScopeCleanupMarker marker, ReferenceQueue<ScopeCleanupMarker> refQueue, Callable<Void> delegate) {
             super(marker, refQueue);
             this.id = id;             
             scopeProviders.add(scopeProvider);
             delegates.add(delegate);
         }
         
         public Integer getId() {
             return id;
         }
         
         public void add(Provider<ScopeCleanupMarker> scopeProvider, Callable<Void> action) {
             delegates.add(0, action);  // add first
             scopeProviders.add(scopeProvider); // hang onto reference
         }
    
         public synchronized Void call() {
             if (!complete) {
                 for (Callable<Void> r : delegates) {
                     try {
                        r.call();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                 }
                 complete = true;
             }
             return null;
         }
     }    
}
