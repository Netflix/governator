package com.netflix.governator;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.governator.annotations.SuppressLifecycleUninitialized;
import com.netflix.governator.spi.LifecycleListener;

/**
 * Manage state for lifecycle listeners
 * 
 * @author elandau
 */
@Singleton
@SuppressLifecycleUninitialized
public final class LifecycleManager {
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleManager.class);
    
    /**
     * Processes unreferenced LifecycleListeners from the referenceQueue, until
     * the 'running' flag is false or interrupted
     * 
     */
    private final class ListenerCleanupWorker implements Runnable {
        public void run() {
            try {
                while (state.get().isRunning()) {
                    Reference<? extends LifecycleListener> ref = unreferencedListenersQueue.remove(1000);
                    if (ref != null && ref instanceof SafeLifecycleListener) { 
                        removeListener((SafeLifecycleListener)ref);
                    }
                }
                LOG.info("LifecycleManager.ListenerCleanupWorker is exiting");
            } 
            catch (InterruptedException e) {
                LOG.info("LifecycleManager.ListenerCleanupWorker is exiting due to thread interrupt");
                Thread.interrupted(); // clear interrupted status
            }                
        }
    }

    
    private final Set<SafeLifecycleListener> listeners = new LinkedHashSet<>();
    private final AtomicReference<State> state = new AtomicReference<>(State.Starting);
    private final ReferenceQueue<LifecycleListener> unreferencedListenersQueue = new ReferenceQueue<>();
    private final ExecutorService reqQueueExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("lifecycle-listener-monitor-%d").build());
    private volatile Throwable failureReason;
    
    public enum State {
        Starting(true),
        Started(true),
        Stopped(false),
        Done(false);
        
        private final boolean running;
        private State(boolean running) {
            this.running = running;
        }
        public boolean isRunning() {
            return running;
        }
    }
    
    public LifecycleManager() {        
        LOG.info("Starting '{}'", this);
        reqQueueExecutor.submit(new ListenerCleanupWorker());
    }
    
    public synchronized void notifyStarted() {
        if (state.compareAndSet(State.Starting, State.Started)) {
            LOG.info("Started '{}'", this);
            for (LifecycleListener listener : listeners()) {
                listener.onStarted();
            }
        }
    }
    
    public synchronized void notifyStartFailed(final Throwable t) {
        // State.Started added here to allow for failure when LifecycleListener.onStarted() is called, post-injector creation
        if (state.compareAndSet(State.Starting, State.Stopped) || state.compareAndSet(State.Started, State.Stopped)) {
            LOG.info("Failed start of '{}'", this);
            this.failureReason = t;
            shutdown(t);
        }
    }

    public synchronized void notifyShutdown() {
        if (state.compareAndSet(State.Started, State.Stopped)) {
            LOG.info("Stopping '{}'", this);
            shutdown(null);
            state.set(State.Done);
        }
    }
    
    private void removeListener(SafeLifecycleListener listenerRef) {
        synchronized(listeners) {
            listeners.remove(listenerRef);
        }
    }
    
    public void addListener(LifecycleListener listener) {
        SafeLifecycleListener safeListener = SafeLifecycleListener.wrap(listener, unreferencedListenersQueue);
        
        boolean listenerAdded;
        synchronized(listeners) {
            listenerAdded = listeners.add(safeListener);
        }
        
        if (listenerAdded) {
            LOG.info("Added listener '{}'", safeListener);
            switch (state.get()) {
            case Started:
                safeListener.onStarted();
                break;
            case Stopped:
                safeListener.onStopped(failureReason);
                break;
            default:
                // ignore
            }
        }
    }
    
    /**
     * safely returns a copy of listeners in multi-threaded environment
     * @return
     */
    private LinkedList<SafeLifecycleListener> listeners() {
        synchronized(listeners) {
            return new LinkedList<>(listeners);
        }
    }
    
    private void shutdown(final Throwable t) {
        reqQueueExecutor.shutdown();
        Iterator<SafeLifecycleListener> shutdownIter = listeners().descendingIterator();
        listeners.clear();        
        while (shutdownIter.hasNext()) {
            shutdownIter.next().onStopped(t);
        }
    }
    
    public State getState() {
        return state.get();
    }
    
    public Throwable getFailureReason() {
        return failureReason;
    }

    @Override
    public String toString() {
        return "LifecycleManager@" + System.identityHashCode(this);
    }
}
