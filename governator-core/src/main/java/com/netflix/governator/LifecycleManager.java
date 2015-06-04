package com.netflix.governator;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manage state for lifecycle listeners
 * 
 * @author elandau
 */
@Singleton
public class LifecycleManager {
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleManager.class);
    
    private final CopyOnWriteArraySet<LifecycleListener> listeners = new CopyOnWriteArraySet<>();
    private final AtomicReference<State> state = new AtomicReference<>(State.Idle);
    
    public enum State {
        Idle,
        Starting,
        Started,
        Stopped,
        Done
    }
    
    @Inject
    public void setListeners(Set<LifecycleListener> listeners) {
        this.listeners.addAll(listeners);
    }
    
    public void addListener(LifecycleListener listener) {
        listeners.add(listener);
    }
    
    void notifyStarting() {
        if (state.compareAndSet(State.Idle, State.Starting)) {
        }
    }
    
    void notifyStarted() {
        if (state.compareAndSet(State.Starting, State.Started)) {
            for (LifecycleListener listener : listeners) {
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
