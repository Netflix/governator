package com.netflix.governator;

import java.util.concurrent.TimeUnit;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.governator.ProvisionMetrics.Element;
import com.netflix.governator.ProvisionMetrics.Visitor;

@Singleton
public class LoggingProvisionMetricsLifecycleListener extends DefaultLifecycleListener {
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
        metrics.accept(new Visitor() {
                int level = 1;
                
                @Override
                public void visit(Element entry) {
                    LOG.info(String.format("%" + (level * 3 - 2) + "s%s%s : %d ms (%d ms)", 
                            "",
                            entry.getKey().getTypeLiteral().toString(), 
                            entry.getKey().getAnnotation() == null ? "" : " [" + entry.getKey().getAnnotation() + "]",
                            entry.getTotalDuration(TimeUnit.MILLISECONDS),
                            entry.getDuration(TimeUnit.MILLISECONDS)
                            ));
                    level++;
                    entry.accept(this);
                    level--;
                }
            });
    }
    
    @Override
    public void onStartFailed(Throwable t) {
        LOG.info("Injection provision report for failed start : \n" );
        metrics.accept(new Visitor() {
                int level = 1;
                
                @Override
                public void visit(Element entry) {
                    LOG.info(String.format("%" + (level * 3 - 2) + "s%s%s : %d ms (%d ms)", 
                            "",
                            entry.getKey().getTypeLiteral().toString(), 
                            entry.getKey().getAnnotation() == null ? "" : " [" + entry.getKey().getAnnotation() + "]",
                            entry.getTotalDuration(TimeUnit.MILLISECONDS),
                            entry.getDuration(TimeUnit.MILLISECONDS)
                            ));
                    level++;
                    entry.accept(this);
                    level--;
                }
            });
    }
}
