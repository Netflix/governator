package com.netflix.governator;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Singleton
public class LoggingProvisionMetricsLifecycleListener extends AbstractLifecycleListener {
    private static final Logger LOG = LoggerFactory.getLogger(LoggingProvisionMetricsLifecycleListener.class);
    
    private final ProvisionMetrics metrics;

    @Inject
    LoggingProvisionMetricsLifecycleListener(LifecycleManager manager, ProvisionMetrics metrics) {
        this.metrics = metrics;
        manager.addListener(this);
    }
    
    @Override
    public void onStarted() {
        LOG.info("Injection provision report : \n" );
        metrics.accept(new LoggingProvisionMetricsVisitor());
    }
    
    @Override
    public void onStopped(Throwable t) {
        if (t != null) {
            LOG.info("Injection provision report for failed start : \n" );
            metrics.accept(new LoggingProvisionMetricsVisitor());
        }
    }
}
