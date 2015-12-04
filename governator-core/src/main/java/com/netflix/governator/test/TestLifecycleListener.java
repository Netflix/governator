package com.netflix.governator.test;

import javax.inject.Inject;

import com.google.inject.Injector;
import com.netflix.governator.LifecycleListener;

public abstract class TestLifecycleListener implements LifecycleListener {
    @Inject
    Injector injector;

    private boolean isStarted = false;
    private boolean isStopped = false;
    private boolean isStartFailed = false;
    private boolean isFinished = false;
    
    @Override
    public void onStarted() {
        isStarted = true;
    }

    @Override
    public void onStopped() {
        isStopped = true;
    }

    @Override
    public void onStartFailed(Throwable t) {
        isStartFailed = true;
    }

    @Override
    public void onFinished() {
        isFinished = true;
        onReady(injector);
    }

    public boolean isStarted() {
        return isStarted;
    }
    
    public boolean isStopped() {
        return isStopped;
    }
    
    public boolean isStartFailed() {
        return isStartFailed;
    }
    
    public boolean isFinished() {
        return isFinished;
    }
    
    protected abstract void onReady(Injector injector);
}
