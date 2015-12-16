package com.netflix.governator;

import java.util.HashSet;
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
    
    private final Set<LifecycleListener> listeners = new HashSet<>();
    private final AtomicReference<State> state = new AtomicReference<>(State.Starting);
    private volatile Throwable failureReason;
    
    public enum State {
        Starting,
        Started,
        Stopped,
        Done
    }
    
    public synchronized void addListener(LifecycleListener listener) {
        listener = SafeLifecycleListener.wrap(listener);
        
        if (!listeners.contains(listener) && listeners.add(listener)) {
            LOG.info("Adding LifecycleListener '{}' {}", listener, System.identityHashCode(listener));
            switch (state.get()) {
            case Started:
                listener.onStarted();
                break;
            case Stopped:
                listener.onStopped(failureReason);
                break;
            }
        }
    }
    
    public synchronized void notifyStarted() {
        if (state.compareAndSet(State.Starting, State.Started)) {
            for (LifecycleListener listener : listeners) {
                listener.onStarted();
            }
        }
    }
    
    public synchronized void notifyStartFailed(Throwable t) {
        if (state.compareAndSet(State.Starting, State.Stopped)) {
            failureReason = t;
            for (LifecycleListener listener : listeners) {
                listener.onStopped(t);
            }
        }
    }
    
    public synchronized void notifyShutdown() {
        if (state.compareAndSet(State.Started, State.Done)) {
            LOG.info("Shutting down LifecycleManager");
            for (LifecycleListener listener : listeners) {
                listener.onStopped(null);
            }
        }
    }
    
    public State getState() {
        return state.get();
    }
    
    public Throwable getFailureReason() {
        return failureReason;
    }
}
