package com.netflix.governator;

import org.junit.Test;
import org.testng.Assert;

public class ProvisionMetricsModuleTest {
    @Test
    public void defaultMetricsAreEmpty() {
        LifecycleInjector injector = InjectorBuilder.fromModules().createInjector();
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
        LifecycleInjector injector = InjectorBuilder.fromModules(new ProvisionMetricsModule())
                .createInjector();
        
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
