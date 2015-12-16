package com.netflix.governator;

import com.netflix.governator.spi.LifecycleListener;

public abstract class AbstractLifecycleListener implements LifecycleListener {
    @Override
    public void onStopped(Throwable t) {
    }

    @Override
    public void onStarted() {
    }
}
