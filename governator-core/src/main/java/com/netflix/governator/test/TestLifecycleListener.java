package com.netflix.governator.test;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.netflix.governator.spi.LifecycleListener;

public abstract class TestLifecycleListener implements LifecycleListener {
    @Inject
    Injector injector;

    private boolean isStarted = false;
    private boolean isStopped = false;
    private Throwable error = null;
    
    @Override
    public void onStarted() {
        isStarted = true;
    }

    @Override
    public void onStopped(Throwable t) {
        error = t;
        isStopped = true;
    }

    public boolean isStarted() {
        return isStarted;
    }
    
    public boolean isStopped() {
        return isStopped;
    }
    
    public Throwable getError() {
        return error;
    }
    
    protected abstract void onReady(Injector injector);
}
