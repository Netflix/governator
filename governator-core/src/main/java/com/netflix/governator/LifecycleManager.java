package com.netflix.governator;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.governator.annotations.SuppressLifecycleUninitialized;
import com.netflix.governator.internal.SafeLifecycleListener;

/**
 * Manage state for lifecycle listeners
 * 
 * @author elandau
 */
@Singleton
@SuppressLifecycleUninitialized
public final class LifecycleManager {
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleManager.class);
    
    private final CopyOnWriteArraySet<LifecycleListener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicReference<State> state = new AtomicReference<>(State.Starting);
    private volatile Throwable failureReason;
    
    public enum State {
        Starting,
        Started,
        Stopped,
        Failed,
        Done
    }
    
    public void addListener(LifecycleListener listener) {
        listener = SafeLifecycleListener.wrap(listener);
        
        if (listeners.add(listener)) {
            LOG.info("Adding LifecycleListener '{}' {}", listener, System.identityHashCode(listener));
            switch (state.get()) {
            case Started:
                listener.onStarted();
                break;
            case Failed:
                listener.onStartFailed(failureReason);
                break;
            case Done:
                listener.onStopped();
                break;
            }
        }
    }
    
    public void notifyStarted() {
        if (state.compareAndSet(State.Starting, State.Started)) {
            for (LifecycleListener listener : listeners) {
                listener.onStarted();
            }
        }
    }
    
    public void notifyStartFailed(Throwable t) {
        if (state.compareAndSet(State.Starting, State.Failed)) {
            failureReason = t;
            for (LifecycleListener listener : listeners) {
                listener.onStartFailed(t);
            }
        }
    }
    
    public void notifyShutdown() {
        if (state.compareAndSet(State.Started, State.Done)) {
            LOG.info("Shutting down LifecycleManager");
            for (LifecycleListener listener : listeners) {
                listener.onStopped();
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
