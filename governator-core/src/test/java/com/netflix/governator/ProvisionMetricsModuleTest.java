package com.netflix.governator;

import org.junit.Test;
import org.testng.Assert;

public class ProvisionMetricsModuleTest {
    @Test
    public void defaultMetricsAreEmpty() {
        LifecycleInjector injector = new Governator().run();
        try {
            ProvisionMetrics metrics = injector.getInstance(ProvisionMetrics.class);
            LoggingProvisionMetricsVisitor visitor = new LoggingProvisionMetricsVisitor();
            metrics.accept(visitor);
            Assert.assertTrue(visitor.getElementCount() == 0);
        }
        finally {
            injector.shutdown();
        }
    }
    
    @Test
    public void installedMetricsHaveData() {
        LifecycleInjector injector = new Governator()
            .addModules(new ProvisionMetricsModule())
            .run();
        
        try {
            ProvisionMetrics metrics = injector.getInstance(ProvisionMetrics.class);
            LoggingProvisionMetricsVisitor visitor = new LoggingProvisionMetricsVisitor();
            metrics.accept(visitor);
            Assert.assertTrue(visitor.getElementCount() > 0);
        }
        finally {
            injector.shutdown();
        }
    }
}
