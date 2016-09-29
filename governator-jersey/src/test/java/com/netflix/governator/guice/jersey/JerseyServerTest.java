package com.netflix.governator.guice.jersey;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.util.Modules;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjector;
import com.netflix.governator.ShutdownHookModule;
import com.netflix.governator.guice.jersey.GovernatorJerseySupportModule;
import com.netflix.governator.guice.jetty.DefaultJettyConfig;
import com.netflix.governator.guice.jetty.GovernatorServletContainer;
import com.netflix.governator.guice.jetty.JettyConfig;
import com.netflix.governator.guice.jetty.JettyModule;
import com.netflix.governator.guice.jetty.resources3.SampleResource;
import com.netflix.governator.providers.Advises;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.function.UnaryOperator;

import javax.inject.Named;
import javax.inject.Singleton;

public class JerseyServerTest {
    @Test
    public void confirmResourceLoadedWithoutBinding() throws InterruptedException, MalformedURLException, IOException {
        // Create the injector and autostart Jetty
        try (LifecycleInjector injector = InjectorBuilder.fromModules(
                new ShutdownHookModule(), 
                new GovernatorJerseySupportModule(),
                new JerseyServletModule() {
                    @Override
                    protected void configureServlets() {
                        serve("/*").with(GovernatorServletContainer.class);
                    }
                    
                    @Advises
                    @Singleton
                    @Named("governator")
                    UnaryOperator<DefaultResourceConfig> getResourceConfig() {
                        return config -> {
                            Map<String, Object> props = config.getProperties();
                            props.put(ResourceConfig.FEATURE_DISABLE_WADL, "false");

                            config.getClasses().add(SampleResource.class);

                            return config;
                        };
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
                        .createInjector()) {
    
            // Determine the emphermal port from jetty
            Server server = injector.getInstance(Server.class);
            int port = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
            
            System.out.println("Listening on port : "+ port);
            
            URL url = new URL(String.format("http://localhost:%d/", port));
            HttpURLConnection conn = (HttpURLConnection)url.openConnection();
            Assert.assertEquals(200,  conn.getResponseCode());
        };
    }
    
    @Test
    public void confirmFailedToCreateWithoutRootResources() throws InterruptedException, MalformedURLException, IOException {
        // Create the injector and autostart Jetty
        try {
            InjectorBuilder.fromModules(
                new ShutdownHookModule(), 
                new GovernatorJerseySupportModule(),
                new JerseyServletModule() {
                    protected void configureServlets() {
                        serve("/*").with(GovernatorServletContainer.class);
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
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getCause().getMessage().contains("The ResourceConfig instance does not contain any root resource classes"));
        }
    }
    
    @Test
    public void confirmNoResourceLoadedFromGuiceBinding() throws InterruptedException, MalformedURLException, IOException {
        try {
            InjectorBuilder.fromModules(
                new ShutdownHookModule(), 
                new GovernatorJerseySupportModule(),
                new JerseyServletModule() {
                    protected void configureServlets() {
                        serve("/*").with(GovernatorServletContainer.class);
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
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getCause().getMessage().contains("The ResourceConfig instance does not contain any root resource classes"));
        }
    }
}
