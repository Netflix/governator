package com.netflix.governator.guice.modules;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;

public class ModuleDepdenciesTest {
    private static final Logger LOG = LoggerFactory.getLogger(ModuleDepdenciesTest.class);
    
    private static AtomicLong counter = new AtomicLong(0);
    
    @Singleton
    public static class ModuleA extends AbstractModule {
        public ModuleA() {
            LOG.info("ModuleA created");
        }
        
        @Override
        protected void configure() {
            LOG.info("ConfigureA");
            counter.incrementAndGet();
        }
    }
    
    @AfterMethod
    public void afterEachTest() {
        counter.set(0);
    }
    
    @Singleton
    public static class ModuleB extends AbstractModule {
        @Inject
        public ModuleB(ModuleA a) {
            LOG.info("ModuleB created");
        }
        
        @Override
        protected void configure() {
            LOG.info("ConfigureB");
            counter.incrementAndGet();
        }
    }
    
    @Test
    public void testModuleDepdency() throws Exception {
        Injector injector = LifecycleInjector.builder()
            .withRootModule(ModuleB.class)
            .build()
            .createInjector();
    }
}
