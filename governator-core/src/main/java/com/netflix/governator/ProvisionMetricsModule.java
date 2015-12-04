package com.netflix.governator;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Singleton;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.ProvisionListener;

public class ProvisionMetricsModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(ProvisionMetricsModule.class);
    
    @Singleton
    private static class MetricsProvisionListener implements ProvisionListener {
        private ProvisionMetrics metrics;

        @Inject
        public static void initialize(
                MetricsProvisionListener listener, 
                ProvisionMetrics metrics) {
            listener.metrics = metrics;
        }
        
        @Override
        public <T> void onProvision(ProvisionInvocation<T> provision) {
            final Key<?> key = provision.getBinding().getKey();
            
            if (metrics == null) {
                LOG.debug("LifecycleProvisionListener not initialized yet : {} source={}", key, provision.getBinding().getSource());
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
    }
    
    private MetricsProvisionListener listener = new MetricsProvisionListener();
    
    @Override
    protected void configure() {
        this.bindListener(Matchers.any(), listener);
        requestStaticInjection(MetricsProvisionListener.class);
        bind(MetricsProvisionListener.class).toInstance(listener);
        bind(ProvisionMetrics.class).to(SimpleProvisionMetrics.class);
    }

}
