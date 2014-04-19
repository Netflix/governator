package com.netflix.governator.jetty;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ws.rs.GET;
import javax.ws.rs.Path;

import junit.framework.Assert;

import org.eclipse.jetty.server.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.google.common.base.Stopwatch;
import com.google.inject.Injector;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.runner.TerminationEvent;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class JettyRunnerTest {
    private static Logger LOG = LoggerFactory.getLogger(JettyRunnerTest.class);

    private final static AtomicBoolean initCalled = new AtomicBoolean();
    private final static AtomicBoolean shutdownCalled = new AtomicBoolean();
    
    @Path("/")
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
        
        @GET
        public String hello() {
        	return "hello";
        }
    }
    
    @BeforeTest
    public static void before() {
        initCalled.set(false);
        shutdownCalled.set(false);
    }
    
    @Test
    public void shouldCreateSingletonAndExitAfter1Second() throws Exception {
        Stopwatch sw = new Stopwatch().start();
        
        Injector injector = LifecycleInjector.builder()
            // Example of a singleton that will be created
            .withAdditionalModules(new JerseyServletModule() {
                @Override
                protected void configureServlets() {
                    bind(SomeSingleton.class).asEagerSingleton();
                    
                    bind(GuiceContainer.class).asEagerSingleton();
                    serve("/*").with(GuiceContainer.class);
                }
            })
            .withAdditionalBootstrapModules(
                JettyRunnerModule.builder()
                    .build())
            .build()
            .createInjector();
        
        TimeUnit.SECONDS.sleep(3);
        
        Server server = injector.getInstance(Server.class);
        Assert.assertTrue(server.isRunning());
        
        TerminationEvent event = injector.getInstance(TerminationEvent.class);
        event.terminate();
        
        TimeUnit.SECONDS.sleep(3);
        
        Assert.assertTrue(server.isStopped());
        
        LOG.info("Exit main");

    }
}
