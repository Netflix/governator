package com.netflix.governator.lifecycle;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.TypeLiteral;

public class LoggingLifecycleListener extends DefaultLifecycleListener {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingLifecycleListener.class);
    
    @Override
    public <T> void objectInjected(TypeLiteral<T> type, T obj) {
        LOG.info("Injected {} {}@{}", new Object[]{
                type.toString(), 
                obj.getClass().getName(), 
                Integer.toHexString(System.identityHashCode(obj))});
    }

    @Override
    public void stateChanged(Object obj, LifecycleState newState) {
    }

    @Override
    public <T> void objectInjected(TypeLiteral<T> type, T obj, long duration, TimeUnit units) {
        LOG.info("Injected {} in {} {}", new Object[]{
                type.toString(), 
                TimeUnit.MILLISECONDS.convert(duration, TimeUnit.NANOSECONDS), 
                TimeUnit.MILLISECONDS});
    }

    @Override
    public <T> void objectInjecting(TypeLiteral<T> type) {
        LOG.info("Injecting {}", new Object[]{type.toString()});
    }
}
