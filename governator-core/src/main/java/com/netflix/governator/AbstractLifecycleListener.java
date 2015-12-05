package com.netflix.governator;


public abstract class AbstractLifecycleListener implements LifecycleListener {
    @Override
    public void onStopped(Throwable t) {
    }

    @Override
    public void onStarted() {
    }
}
