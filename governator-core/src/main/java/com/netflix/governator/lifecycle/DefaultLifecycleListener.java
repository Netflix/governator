package com.netflix.governator.lifecycle;

import java.util.concurrent.TimeUnit;

import com.google.inject.TypeLiteral;

public class DefaultLifecycleListener implements LifecycleListener {
    @Override
    public <T> void objectInjected(TypeLiteral<T> type, T obj) {
    }

    @Override
    public <T> void objectInjected(TypeLiteral<T> type, T obj, long duration, TimeUnit units) {
    }

    @Override
    public void stateChanged(Object obj, LifecycleState newState) {
    }

    @Override
    public <T> void objectInjecting(TypeLiteral<T> type) {
    }
}
