package com.netflix.governator.guice.jetty;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.testng.annotations.Test;

import com.google.common.collect.ImmutableMap;
import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Provides;
import com.google.inject.util.Modules;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjector;
import com.netflix.governator.ShutdownHookModule;
import com.sun.jersey.api.core.PackagesResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class JerseyServerTest {
    @Test
    public void confirmResourceLoadedWithoutBinding() throws InterruptedException, MalformedURLException, IOException {
        // Create the injector and autostart Jetty
        LifecycleInjector injector = InjectorBuilder.fromModules(
                new ShutdownHookModule(), 
                new JerseyServletModule() {
                    @Override
                    protected void configureServlets() {
                        bind(GuiceContainer.class).to(GovernatorServletContainer.class).asEagerSingleton();
                        serve("/*").with(GuiceContainer.class);
                    }
                    
                    @Provides
                    ResourceConfig getResourceConfig() {
                        return new PackagesResourceConfig(ImmutableMap.<String, Object>builder()
                                .put(PackagesResourceConfig.PROPERTY_PACKAGES, SampleResource.class.getPackage().getName())
                                .put(ResourceConfig.FEATURE_DISABLE_WADL, "false")
                                .build());
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
                new JerseyServletModule() {
                    protected void configureServlets() {
                        bind(GuiceContainer.class).to(GovernatorServletContainer.class).asEagerSingleton();
                        serve("/*").with(GuiceContainer.class);
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
                new JerseyServletModule() {
                    protected void configureServlets() {
                        bind(GuiceContainer.class).to(GovernatorServletContainer.class).asEagerSingleton();
                        serve("/*").with(GuiceContainer.class);
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
