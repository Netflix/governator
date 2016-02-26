package com.netflix.governator;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Provides;
import com.google.inject.util.Modules;
import com.netflix.governator.guice.jetty.DefaultJettyConfig;
import com.netflix.governator.guice.jetty.GovernatorJerseyServletModule;
import com.netflix.governator.guice.jetty.JettyConfig;
import com.netflix.governator.guice.jetty.JettyModule;
import com.netflix.governator.jersey.annotations.ServletContainerProperties;
import com.sun.jersey.api.core.PackagesResourceConfig;

public class JettyServerTest {
    @Test
    public void confirmResourceLoadedWithoutBinding() throws InterruptedException, MalformedURLException, IOException {
        // Create the injector and autostart Jetty
        LifecycleInjector injector = InjectorBuilder.fromModules(
                new ShutdownHookModule(), 
                new GovernatorJerseyServletModule() {
                    @Override
                    protected void configureMoreServlets() {
                    }
                    
                    @Provides
                    @ServletContainerProperties
                    Map<String, Object> getContainerProperties() {
                        return ImmutableMap.<String, Object>builder()
                                .put(PackagesResourceConfig.PROPERTY_PACKAGES, "com.netflix")
                                .build();
                    }
                },
                Modules.override(new JettyModule())
                       .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                            }
                            
                            @Provides
                            JettyConfig getConfig() {
                                // Use emphemeral ports
                                return new DefaultJettyConfig().setPort(0);
                            }
                        }))
                        .createInjector();

        // Determine the emphermal port from jetty
        Server server = injector.getInstance(Server.class);
        int port = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
        
        System.out.println("Listening on port : "+ port);
        
        URL url = new URL(String.format("http://localhost:%d/", port));
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        try {
            conn.getResponseCode();
        }
        catch (Exception e) {
            
        }
        injector.shutdown();
    }
    
    @Test(expectedExceptions={CreationException.class})
    public void confirmFailedToCreateWithoutRootResources() throws InterruptedException, MalformedURLException, IOException {
        // Create the injector and autostart Jetty
        LifecycleInjector injector = InjectorBuilder.fromModules(
                new ShutdownHookModule(), 
                new GovernatorJerseyServletModule() {
                    @Override
                    protected void configureMoreServlets() {
                    }
                },
                Modules.override(new JettyModule())
                       .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                            }
                            
                            @Provides
                            JettyConfig getConfig() {
                                // Use emphemeral ports
                                return new DefaultJettyConfig().setPort(0);
                            }
                        }))
                        .createInjector();

        // Determine the emphermal port from jetty
        Server server = injector.getInstance(Server.class);
        int port = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
        
        System.out.println("Listening on port : "+ port);
        
        URL url = new URL(String.format("http://localhost:%d/", port));
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        try {
            conn.getResponseCode();
        }
        catch (Exception e) {
            
        }
        injector.shutdown();
    }
    
    @Test
    public void confirmResourceLoadedWithBindingOnly() throws InterruptedException, MalformedURLException, IOException {
        // Create the injector and autostart Jetty
        LifecycleInjector injector = InjectorBuilder.fromModules(
                new ShutdownHookModule(), 
                new GovernatorJerseyServletModule() {
                    @Override
                    protected void configureMoreServlets() {
                        bind(SampleResource.class);
                    }
                },
                Modules.override(new JettyModule())
                       .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                            }
                            
                            @Provides
                            JettyConfig getConfig() {
                                // Use emphemeral ports
                                return new DefaultJettyConfig().setPort(0);
                            }
                        }))
                        .createInjector();

        // Determine the emphermal port from jetty
        Server server = injector.getInstance(Server.class);
        int port = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
        
        System.out.println("Listening on port : "+ port);
        
        URL url = new URL(String.format("http://localhost:%d/", port));
        HttpURLConnection conn = (HttpURLConnection)url.openConnection();
        try {
            conn.getResponseCode();
        }
        catch (Exception e) {
            
        }
        injector.shutdown();
    }
}
