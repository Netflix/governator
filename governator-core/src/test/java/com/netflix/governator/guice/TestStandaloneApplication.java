package com.netflix.governator.guice;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import junit.framework.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.base.Stopwatch;
import com.google.inject.AbstractModule;
import com.netflix.governator.guice.runner.TerminationEvent;
import com.netflix.governator.guice.runner.events.SelfDestructingTerminationEvent;
import com.netflix.governator.guice.runner.standalone.StandaloneRunnerModule;

public class TestStandaloneApplication {
    private static Logger LOG = LoggerFactory.getLogger(TestStandaloneApplication.class);
    
    private final static AtomicBoolean initCalled = new AtomicBoolean();
    private final static AtomicBoolean shutdownCalled = new AtomicBoolean();
    
    public static class SomeSingleton {
        @PostConstruct
        public void init() {
            LOG.info("Init SomeSingleton()");
            initCalled.set(true);
        }
        
        @PreDestroy
        public void shutdown() {
            LOG.info("Shutdown SomeSingleton()");
            shutdownCalled.set(true);
        }
    }
    
    @BeforeTest
    public static void before() {
        initCalled.set(false);
        shutdownCalled.set(false);
    }
    
    @Test(enabled=false)
    public void shouldCreateSingletonAndExitAfter1Second() throws Exception {
        Stopwatch sw = new Stopwatch().start();
        
        final TerminationEvent event = new SelfDestructingTerminationEvent(1, TimeUnit.SECONDS);
        LifecycleInjector.builder()
            // Example of a singleton that will be created
            .withAdditionalModules(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(SomeSingleton.class).asEagerSingleton();
                }
            })
            .withAdditionalBootstrapModules(
                StandaloneRunnerModule.builder()
                    .withTerminateEvent(event)
                    .build())
            .build()
            .createInjector();
        
        event.await();
        long elapsed = sw.elapsed(TimeUnit.MILLISECONDS);
        LOG.info("Elapsed: " + elapsed);
        Assert.assertTrue(initCalled.get());
        Assert.assertTrue(shutdownCalled.get());
        Assert.assertTrue(elapsed > 1000);
        
        LOG.info("Exit main");

    }
    
    public static void main(String args[]) {
        final TerminationEvent event = new SelfDestructingTerminationEvent(1, TimeUnit.SECONDS);
        LifecycleInjector.builder()
            // Example of a singleton that will be created
            .withAdditionalModules(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(SomeSingleton.class).asEagerSingleton();
                }
            })
            .withAdditionalBootstrapModules(
                StandaloneRunnerModule.builder()
                    .withTerminateEvent(event)
                    .build())
            .build()
            .createInjector();
        
    }
}
