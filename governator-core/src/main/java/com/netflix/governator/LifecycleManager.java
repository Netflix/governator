package com.netflix.governator;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
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
                while (running.get()) {
                    Reference<? extends LifecycleListener> ref = unreferencedListenersQueue.remove(1000);
                    if (ref != null && ref instanceof SafeLifecycleListener) { 
                        removeListener((SafeLifecycleListener)ref);
                    }
                }
                LOG.info("LifecycleManager.ListenerCleanupWorker is exiting");
            } 
            catch (InterruptedException e) {
                LOG.info("LifecycleManager.ListenerCleanupWorker is exiting due to thread interrupt");
            }                
        }
    }

    
    private final Set<SafeLifecycleListener> listeners = new LinkedHashSet<>();
    private final AtomicReference<State> state;
    private final ReferenceQueue<LifecycleListener> unreferencedListenersQueue = new ReferenceQueue<>();
    private volatile Throwable failureReason;
    private final ExecutorService reqQueueExecutor = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).setNameFormat("lifecycle-listener-monitor-%d").build());
    private final AtomicBoolean running= new AtomicBoolean(true);
    
    public enum State {
        Starting,
        Started,
        Stopped,
        Done
    }
    
    public LifecycleManager() {        
        LOG.info("Starting '{}'", this);
        state = new AtomicReference<>(State.Starting);      
        reqQueueExecutor.submit(new ListenerCleanupWorker());
    }
    
    private synchronized void removeListener(SafeLifecycleListener listenerRef) {
        listeners.remove(listenerRef);
    }
    
    public synchronized void addListener(LifecycleListener listener) {
        SafeLifecycleListener safeListener = SafeLifecycleListener.wrap(listener, unreferencedListenersQueue);
        
        if (!listeners.contains(safeListener) && listeners.add(safeListener)) {
            LOG.info("Adding listener '{}'", safeListener);
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
    
    public synchronized void notifyStarted() {
        if (state.compareAndSet(State.Starting, State.Started)) {
            LOG.info("Started '{}'", this);
            for (LifecycleListener listener : new ArrayList<>(listeners)) {
                listener.onStarted();
            }
        }
    }
    
    public synchronized void notifyStartFailed(final Throwable t) {
        // State.Started added here to allow for failure  when LifecycleListener.onStarted() is called, post-injector creation
        if (state.compareAndSet(State.Starting, State.Stopped) || state.compareAndSet(State.Started, State.Stopped)) {
            LOG.info("Failed start of '{}'", this);
            if (running.compareAndSet(true, false)) {
                reqQueueExecutor.shutdown();            
            }
            this.failureReason = t;
            Iterator<SafeLifecycleListener> shutdownIter = new LinkedList<>(listeners).descendingIterator();
            while (shutdownIter.hasNext()) {
                shutdownIter.next().onStopped(t);
            }
            listeners.clear();
        }
        state.set(State.Done);        
    }
    
    public synchronized void notifyShutdown() {
        if (running.compareAndSet(true, false)) {
            reqQueueExecutor.shutdown();            
        }
        if (state.compareAndSet(State.Started, State.Stopped)) {
            LOG.info("Stopping '{}'", this);
            Iterator<SafeLifecycleListener> shutdownIter = new LinkedList<>(listeners).descendingIterator();
            while (shutdownIter.hasNext()) {
                shutdownIter.next().onStopped(null);
            }
            listeners.clear();
        }
        state.set(State.Done);
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
