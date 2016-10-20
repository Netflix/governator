package com.netflix.governator;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.ProvisionListener;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;

public final class ProvisionMetricsModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(ProvisionMetricsModule.class);
    
    @Singleton
    private static class MetricsProvisionListener implements ProvisionListener, com.netflix.governator.spi.LifecycleListener {
        private ProvisionMetrics metrics;
        
        private boolean doneLoading = false;
        
        @Inject
        public static void initialize(MetricsProvisionListener listener, ProvisionMetrics metrics)  {
            listener.metrics = metrics;
        }
        
        @Override
        public <T> void onProvision(ProvisionInvocation<T> provision) {
            final Key<?> key = provision.getBinding().getKey();
            
            if (metrics == null) {
                LOG.debug("LifecycleProvisionListener not initialized yet : {} source={}", key, provision.getBinding().getSource());
                return;
            }
            
            if (doneLoading) {
                return;
            }
            
            // Instantiate the type and pass to the metrics.  This time captured will
            // include invoking any lifecycle events.
            metrics.push(key);
            try {
                provision.provision();
            }
            finally {
                metrics.pop();
            }
        }
        
        @Override
        public void onStarted() {
            doneLoading = true;
        }

        @Override
        public void onStopped(Throwable error) {
            doneLoading = true;
        }
    }
    
    private MetricsProvisionListener listener = new MetricsProvisionListener();
    
    @Override
    protected void configure() {
        bindListener(Matchers.any(), listener);
        requestStaticInjection(MetricsProvisionListener.class);
    }
    
    @Provides
    @Singleton
    MetricsProvisionListener getMetricsProvisionListener() {
        return listener;
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return getClass().equals(obj.getClass());
    }
    
    @Override
    public String toString() {
        return "ProvisionMetricsModule[]";
    }
}
