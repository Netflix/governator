package com.netflix.governator.guice.jetty;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjector;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;

public class PreDestroySingletonScopeBugTest {
    private static final Logger LOG = LoggerFactory.getLogger(PreDestroySingletonScopeBugTest.class);
    
    @Test
    public void callingScopeOnSingletonDoesntBlowUp() throws Exception {
        LifecycleInjector injector = InjectorBuilder
            .fromModules(
                new JerseyServletModule() {
                    @Override
                    protected void configureServlets() {
                        serve("/*").with(GuiceContainer.class, Collections.singletonMap(
                            PackagesResourceConfig.PROPERTY_PACKAGES, "com.netflix.governator.guice.jetty.resources2"));
                    }
                }, 
                new JettyModule())
            .overrideWith(new AbstractModule() {
                @Override
                protected void configure() {
                }
                
                @Provides
                JettyConfig getConfig() {
                    // Use emphemeral ports
                    return new DefaultJettyConfig().setPort(0);
                }
            })
            .createInjector();
        
        LOG.info("-----------------------------------");
        
        Server server = injector.getInstance(Server.class);
        int port = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
        
        LOG.info("Port : " + port);
        
        URL url = new URL(String.format("http://localhost:%d/hello", port));
        HttpURLConnection conn;
        conn = (HttpURLConnection)url.openConnection();
        Assert.assertEquals(200, conn.getResponseCode());

        conn = (HttpURLConnection)url.openConnection();
        LOG.info("Response : " + conn.getResponseCode());
        Assert.assertEquals(200, conn.getResponseCode());
        
        injector.close();
    }
}
