package com.netflix.governator.guice.jetty;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import javax.annotation.PreDestroy;

import junit.framework.Assert;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.util.Modules;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjector;
import com.netflix.governator.ShutdownHookModule;
import com.netflix.governator.guice.jetty.DefaultJettyConfig;
import com.netflix.governator.guice.jetty.JettyConfig;
import com.netflix.governator.guice.jetty.JettyModule;

public class JettyServerTest {
    static class Foo {
        private boolean shutdownCalled;
        
        @PreDestroy
        void shutdown() {
            shutdownCalled = true;
        }
    };
    
    
    @Test
    public void confirmShutdownSequence() throws InterruptedException, MalformedURLException, IOException {
        // Create the injector and autostart Jetty
        LifecycleInjector injector = InjectorBuilder.fromModules(
                new SampleServletModule(), 
                new ShutdownHookModule(), 
                Modules.override(new JettyModule())
                       .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                                bind(Foo.class).asEagerSingleton();
                            }
                            
                            @Provides
                            JettyConfig getConfig() {
                                // Use emphemeral ports
                                return new DefaultJettyConfig().setPort(0);
                            }
                        }))
                        .createInjector();

        Foo foo = injector.getInstance(Foo.class);
        
        // Determine the emphermal port from jetty
        Server server = injector.getInstance(Server.class);
        int port = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
        
        SampleResource resource = injector.getInstance(SampleResource.class);
        Assert.assertEquals(1, resource.getPostConstructCount());
        Assert.assertEquals(0, resource.getPreDestroyCount());
        
        System.out.println("Listening on port : "+ port);
        URL url = new URL(String.format("http://localhost:%d/kill", port));
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        try {
            conn.getResponseCode();
        }
        catch (Exception e) {
            
        }
        injector.awaitTermination();
        
        Assert.assertTrue(foo.shutdownCalled);
        Assert.assertEquals(1, resource.getPostConstructCount());
        Assert.assertEquals(1, resource.getPreDestroyCount());
    }
}
