package com.netflix.governator;

import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    
    private final Set<LifecycleListener> listeners = new LinkedHashSet<>();
    private final AtomicReference<State> state;
    private volatile Throwable failureReason;
    
    public enum State {
        Starting,
        Started,
        Stopped,
        Done
    }
    
    public LifecycleManager() {        
        LOG.info("Starting '{}'", this);
        state = new AtomicReference<>(State.Starting);               
    }
    
    public synchronized void addListener(LifecycleListener listener) {
        listener = SafeLifecycleListener.wrap(listener);
        
        if (!listeners.contains(listener) && listeners.add(listener)) {
            LOG.info("Adding listener '{}'", listener);
            switch (state.get()) {
            case Started:
                listener.onStarted();
                break;
            case Stopped:
                listener.onStopped(failureReason);
                break;
            default:
                // ignore
            }
        }
    }
    
    public synchronized void notifyStarted() {
        if (state.compareAndSet(State.Starting, State.Started)) {
            LOG.info("Started '{}'", this);
            for (LifecycleListener listener : listeners) {
                listener.onStarted();
            }
        }
    }
    
    public synchronized void notifyStartFailed(final Throwable t) {
        // State.Started added here to allow for failure  when LifecycleListener.onStarted() is called, post-injector creation
        if (state.compareAndSet(State.Starting, State.Stopped) || state.compareAndSet(State.Started, State.Stopped)) {
            LOG.info("Failed start of '{}'", this);
            this.failureReason = t;
            Iterator<LifecycleListener> shutdownIter = new LinkedList<>(listeners).descendingIterator();
            while (shutdownIter.hasNext()) {
                shutdownIter.next().onStopped(t);
            }
        }
    }
    
    public synchronized void notifyShutdown() {
        if (state.compareAndSet(State.Started, State.Stopped)) {
            LOG.info("Stopping '{}'", this);
            Iterator<LifecycleListener> shutdownIter = new LinkedList<>(listeners).descendingIterator();
            while (shutdownIter.hasNext()) {
                shutdownIter.next().onStopped(null);
            }
            state.set(State.Done);
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
