package com.netflix.governator;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage state for lifecycle listeners
 * 
 * @author elandau
 */
@Singleton
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
        if (listeners.add(listener)) {
            LOG.info("Adding LifecycleListener '{}' {}", listener.getClass().getName(), System.identityHashCode(listener));
            switch (state.get()) {
            case Started:
                LOG.info("Starting LifecycleListener '{}'", listener.getClass().getName());
                listener.onStarted();
                break;
            case Failed:
                LOG.info("Failed LifecycleListener '{}'", listener.getClass().getName());
                listener.onStartFailed(failureReason);
                break;
            case Done:
                LOG.info("Stopped LifecycleListener '{}'", listener.getClass().getName());
                listener.onStopped();
                break;
            }
        }
    }
    
    public void notifyStarted() {
        if (state.compareAndSet(State.Starting, State.Started)) {
            for (LifecycleListener listener : listeners) {
                LOG.info("Starting LifecycleListener '{}'", listener.getClass().getName());
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
                try {
                    LOG.info("Stopping LifecycleListener '{}'", listener.getClass().getName());
                    listener.onStopped();
                }
                catch (Exception e) {
                    LOG.error("Failed to shutdown listener {}", listener, e);
                }
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
