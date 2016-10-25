package com.netflix.governator;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.netflix.governator.ProvisionMetrics.Element;
import com.netflix.governator.ProvisionMetrics.Visitor;
import com.netflix.governator.visitors.ProvisionListenerTracingVisitor;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

public class ProvisionMetricsModuleTest {
    @Test
    public void disableMetrics() {
        try (LifecycleInjector injector = InjectorBuilder.fromModule(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(ProvisionMetrics.class).to(NullProvisionMetrics.class);
                    }
                })
            .createInjector()) {
            
            ProvisionMetrics metrics = injector.getInstance(ProvisionMetrics.class);
            LoggingProvisionMetricsVisitor visitor = new LoggingProvisionMetricsVisitor();
            metrics.accept(visitor);
            Assert.assertTrue(visitor.getElementCount() == 0);
        }
    }
    
    @Test
    public void confirmDedupWorksWithOverride() {
        try (LifecycleInjector injector = InjectorBuilder.fromModule(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        install(new ProvisionMetricsModule());
                    }
                })
                // Confirm that installing ProvisionMetricsModule twice isn't broken with overrides
                .overrideWith(new AbstractModule() {
                    @Override
                    protected void configure() {
                    }
                })
            .createInjector()) {
            
            ProvisionMetrics metrics = injector.getInstance(ProvisionMetrics.class);
            LoggingProvisionMetricsVisitor visitor = new LoggingProvisionMetricsVisitor();
            metrics.accept(visitor);
            Assert.assertTrue(visitor.getElementCount() != 0);
        }
    }
    
    @Singleton
    public static class Foo {
        public Foo() throws InterruptedException {
            TimeUnit.MILLISECONDS.sleep(200);
        }
        
        @PostConstruct
        public void init() throws InterruptedException {
            TimeUnit.MILLISECONDS.sleep(200);
        }
    }
    
    public class KeyTrackingVisitor implements Visitor {
        private Element element;
        private Key key;
        
        KeyTrackingVisitor(Key key) {
            this.key = key;
        }

        @Override
        public void visit(Element element) {
            if (element.getKey().equals(key)) {
                this.element = element;
            }
        }
        
        Element getElement() {
            return element;
        }
    }
    @Test
    public void confirmMetricsIncludePostConstruct() {
        try (LifecycleInjector injector = InjectorBuilder.fromModules(
                new ProvisionDebugModule(), 
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(Foo.class).asEagerSingleton();
                    }
                })
            .traceEachElement(new ProvisionListenerTracingVisitor())
            .createInjector()) {
            
            ProvisionMetrics metrics = injector.getInstance(ProvisionMetrics.class);
            KeyTrackingVisitor keyTracker = new KeyTrackingVisitor(Key.get(Foo.class));
            metrics.accept(keyTracker);
            
            Assert.assertNotNull(keyTracker.getElement());
            Assert.assertTrue(keyTracker.getElement().getTotalDuration(TimeUnit.MILLISECONDS) > 300);
        }
    }
}
