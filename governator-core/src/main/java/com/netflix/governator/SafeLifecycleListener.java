package com.netflix.governator;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.governator.spi.LifecycleListener;

/**
 * Wrapper for any LifecycleListener to provide this following funcionality
 * 1.  Logging of events as INFO
 * 2.  Swallow any event handler exceptions during shutdown
 */
final class SafeLifecycleListener implements LifecycleListener {
    private static final Logger LOG = LoggerFactory.getLogger(SafeLifecycleListener.class);

    private final LifecycleListener delegate;

    public static SafeLifecycleListener wrap(LifecycleListener listener) {
        return new SafeLifecycleListener(listener);
    }
    
    private SafeLifecycleListener(LifecycleListener delegate) {
        this.delegate = delegate;
    }
    
    @Override
    public void onStarted() {
        LOG.info("Starting LifecycleListener '{}'", delegate);
        delegate.onStarted();
    }

    @Override
    public void onStopped(Throwable t) {
        LOG.info("Stopping LifecycleListener '{}'", delegate, t);
        try {
            delegate.onStopped(t);
        }
        catch (Exception e) {
            LOG.info("onStopped failed for listener {}", delegate, e);
        }
    }

    @Override
    public String toString() {
        return "Safe[" + delegate.toString() + "]";
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        SafeLifecycleListener other = (SafeLifecycleListener) obj;
        return !delegate.equals(other.delegate);
    }
    

}
