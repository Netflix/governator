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
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Monitors managed instances and invokes cleanup actions when they become unreferenced
 * 
 * @author tcellucci
 *
 */
public class PreDestroyMonitor implements AutoCloseable {
    private static Logger LOGGER = LoggerFactory.getLogger(PreDestroyMonitor.class);
    private static class ScopeCleanupMarker {
        static final Key<ScopeCleanupMarker> MARKER_KEY = Key.get(ScopeCleanupMarker.class);
        // simple id uses identity equality
        private final Object id = new Object();
        private final ScopeCleanupAction cleanupAction;

        public ScopeCleanupMarker(ReferenceQueue<ScopeCleanupMarker> markerReferenceQueue) {
            this.cleanupAction = new ScopeCleanupAction(this, markerReferenceQueue);
        }

        Object getId() {
            return id;
        }

        public ScopeCleanupAction getCleanupAction() {
            return cleanupAction;
        }
    }

    static final class ScopeCleaner implements Provider<ScopeCleanupMarker> {
        ConcurrentMap<Object, ScopeCleanupAction> scopedCleanupActions = new ConcurrentHashMap<>(BinaryConstant.I14_16384);
        ReferenceQueue<ScopeCleanupMarker> markerReferenceQueue = new ReferenceQueue<>();
        final ExecutorService reqQueueExecutor = Executors.newSingleThreadExecutor(
                new ThreadFactoryBuilder().setDaemon(true).setNameFormat("predestroy-monitor-%d").build());
        final AtomicBoolean running = new AtomicBoolean(true);
        final ScopeCleanupMarker singletonMarker = get();

        {
            this.reqQueueExecutor.submit(new ScopedCleanupWorker());
        }

        @Override
        public ScopeCleanupMarker get() {
            ScopeCleanupMarker marker = new ScopeCleanupMarker(markerReferenceQueue);
            scopedCleanupActions.put(marker.getId(), marker.getCleanupAction());
            return marker;
        }

        public boolean isRunning() {
            return running.get();
        }

        public boolean close() throws Exception {
            boolean rv = running.compareAndSet(true, false);
            if (rv) {
                reqQueueExecutor.shutdown(); // executor to stop
                // process any remaining scoped cleanup actions
                List<ScopeCleanupAction> values = new ArrayList<>(scopedCleanupActions.values());
                scopedCleanupActions.clear();
                Collections.sort(values);
                for (Callable<Void> actions : values) {
                    actions.call();
                }

                // make sure executor thread really ended
                if (!reqQueueExecutor.awaitTermination(90, TimeUnit.SECONDS)) {
                    LOGGER.error("internal executor still active; shutting down now");
                    reqQueueExecutor.shutdownNow();
                }
                markerReferenceQueue = null;
            }
            return rv;
        }

        /**
         * Processes unreferenced markers from the referenceQueue, until the 'running' flag is false or interrupted
         * 
         */
        final class ScopedCleanupWorker implements Runnable {
            public void run() {
                try {
                    while (running.get()) {
                        Reference<? extends ScopeCleanupMarker> ref = markerReferenceQueue.remove(1000);
                        if (ref != null && ref instanceof ScopeCleanupAction) {
                            Object markerKey = ((ScopeCleanupAction) ref).getId();
                            ScopeCleanupAction cleanupAction = scopedCleanupActions.remove(markerKey);
                            if (cleanupAction != null) {
                                cleanupAction.call();
                            }
                        }
                    }
                    LOGGER.info("PreDestroyMonitor.ScopedCleanupWorker is exiting");
                } catch (InterruptedException e) {
                    LOGGER.info("PreDestroyMonitor.ScopedCleanupWorker is exiting due to thread interrupt");
                    Thread.currentThread().interrupt(); // clear interrupted status
                }
            }
        }

    }

    private Deque<Callable<Void>> cleanupActions = new ConcurrentLinkedDeque<>();
    private ScopeCleaner scopeCleaner = new ScopeCleaner();

    Map<Class<? extends Annotation>, Scope> scopeBindings;

    public PreDestroyMonitor(Map<Class<? extends Annotation>, Scope> scopeBindings) {
        this.scopeBindings = new HashMap<>(scopeBindings);
    }

    public <T> boolean register(T destroyableInstance, Binding<T> binding, Iterable<LifecycleAction> action) {
        return scopeCleaner.isRunning() ? binding.acceptScopingVisitor(
                new ManagedInstanceScopingVisitor(destroyableInstance, binding.getSource(), action)) : false;
    }

    /*
     * compatibility-mode - scope is assumed to be eager singleton
     */
    public <T> boolean register(T destroyableInstance, Object context, Iterable<LifecycleAction> action) {
        return scopeCleaner.isRunning()
                ? new ManagedInstanceScopingVisitor(destroyableInstance, context, action).visitEagerSingleton() : false;
    }

    /**
     * allows late-binding of scopes to PreDestroyMonitor, useful if more than one Injector contributes scope bindings
     * 
     * @param bindings
     *            additional annotation-to-scope bindings to add
     */
    public void addScopeBindings(Map<Class<? extends Annotation>, Scope> bindings) {
        if (scopeCleaner.isRunning()) {
            scopeBindings.putAll(bindings);
        }
    }

    /**
     * final cleanup of managed instances if any
     */
    @Override
    public void close() throws Exception {
        if (scopeCleaner.close()) { // executor thread to exit processing loop
            LOGGER.info("closing PreDestroyMonitor...");

            for (Callable<Void> action : cleanupActions) {
                action.call();
            }
            cleanupActions.clear();
            scopeBindings.clear();
            scopeBindings = Collections.emptyMap();
        }
        else {
            LOGGER.warn("PreDestroyMonitor.close() invoked but instance is not running");
        }
    }

    /**
     * visits bindingScope of managed instance to set up an appropriate strategy for cleanup, adding actions to either
     * the scopedCleanupActions map or cleanupActions list. Returns true if cleanup actions were added, false if no
     * cleanup strategy was selected.
     * 
     */
    private final class ManagedInstanceScopingVisitor implements BindingScopingVisitor<Boolean> {
        private final Object injectee;
        private final Object context;
        private final Iterable<LifecycleAction> lifecycleActions;

        private ManagedInstanceScopingVisitor(Object injectee, Object context,
                Iterable<LifecycleAction> lifecycleActions) {
            this.injectee = injectee;
            this.context = context;
            this.lifecycleActions = lifecycleActions;
        }

        /*
         * handle eager singletons same as singletons for cleanup purposes.
         * 
         */
        @Override
        public Boolean visitEagerSingleton() {                    
            return visitScope(Scopes.SINGLETON);
        }

        /*
         * use ScopeCleanupMarker dereferencing strategy to detect scope closure, add new entry to scopedCleanupActions
         * map
         * 
         */
        @Override
        public Boolean visitScope(Scope scope) {
            final Provider<ScopeCleanupMarker> scopedMarkerProvider;
            if (scope.equals(Scopes.SINGLETON) || (scope instanceof AbstractScope && ((AbstractScope)scope).isSingletonScope())) {
                scopedMarkerProvider = Providers.of(scopeCleaner.singletonMarker);
            } else {
                scopedMarkerProvider = scope.scope(ScopeCleanupMarker.MARKER_KEY, scopeCleaner);                
            }
            ScopeCleanupMarker marker = scopedMarkerProvider.get();
            marker.getCleanupAction().add(scopedMarkerProvider, new ManagedInstanceAction(injectee, lifecycleActions));
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
         * add a soft-reference ManagedInstanceAction to cleanupActions deque. Cleanup triggered only at injector
         * shutdown if referent has not yet been collected.
         * 
         */
        @Override
        public Boolean visitNoScoping() {
            cleanupActions.addFirst(
                    new ManagedInstanceAction(new SoftReference<Object>(injectee), context, lifecycleActions));
            return true;
        }
    }

    /**
     * Runnable that weakly references a scopeCleanupMarker and strongly references a list of delegate runnables. When
     * the marker is unreferenced, delegates will be invoked in the reverse order of addition.
     */
    private static final class ScopeCleanupAction extends WeakReference<ScopeCleanupMarker>
            implements Callable<Void>, Comparable<ScopeCleanupAction> {
        private volatile static long instanceCounter = 0;
        private final Object id;
        private final long ordinal;
        private Deque<Object[]> delegates = new ConcurrentLinkedDeque<>();
        private final AtomicBoolean complete = new AtomicBoolean(false);

        public ScopeCleanupAction(ScopeCleanupMarker marker, ReferenceQueue<ScopeCleanupMarker> refQueue) {
            super(marker, refQueue);
            this.id = marker.getId();
            this.ordinal = instanceCounter++;
        }

        public Object getId() {
            return id;
        }

        public void add(Provider<ScopeCleanupMarker> scopeProvider, Callable<Void> action) {
            if (!complete.get()) {
                delegates.addFirst(new Object[] { action, scopeProvider }); // add first
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        public Void call() {
            if (complete.compareAndSet(false, true) && delegates != null) {
                for (Object[] r : delegates) {
                    try {
                        ((Callable<Void>) r[0]).call();
                    } catch (Exception e) {
                        LOGGER.error("PreDestroy call failed for " + r, e);
                    }
                }
                delegates.clear();
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
