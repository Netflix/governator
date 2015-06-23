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
    
    public enum State {
        Starting,
        Started,
        Stopped,
        Done
    }
    
    public void addListener(LifecycleListener listener) {
        LOG.info("Adding LifecycleListener '{}'", listener.getClass().getName());
        listeners.add(listener);
        if (state.equals(State.Started)) {
            LOG.info("Starting LifecycleListener '{}'", listener.getClass().getName());
            listener.onStarted();
        }
    }
    
    void notifyStarted() {
        if (state.compareAndSet(State.Starting, State.Started)) {
            for (LifecycleListener listener : listeners) {
                LOG.info("Starting LifecycleListener '{}'", listener.getClass().getName());
                listener.onStarted();
            }
        }
    }
    
    void notifyStartFailed(Throwable t) {
        if (state.compareAndSet(State.Starting, State.Done)) {
            for (LifecycleListener listener : listeners) {
                listener.onStartFailed(t);
            }
        }
    }
    
    void notifyShutdown() {
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
}
