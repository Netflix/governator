package com.netflix.governator.guice.jersey;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Proxy;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.function.UnaryOperator;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Assert;
import org.junit.Test;

import com.google.common.io.CharStreams;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.util.Modules;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjector;
import com.netflix.governator.ShutdownHookModule;
import com.netflix.governator.guice.jetty.DefaultJettyConfig;
import com.netflix.governator.guice.jetty.JettyConfig;
import com.netflix.governator.guice.jetty.JettyModule;
import com.netflix.governator.guice.jetty.resources3.SampleResource;
import com.netflix.governator.providers.Advises;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.JerseyServletModule;

public class JerseyServerTest {
    @Test
    public void confirmResourceLoadedWithoutBinding() throws IOException {
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
    public void confirmFailedToCreateWithoutRootResources() {
        // Create the injector and autostart Jetty
        try (LifecycleInjector injector = InjectorBuilder.fromModules(
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
                        .createInjector()) {
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getCause().getMessage().contains("The ResourceConfig instance does not contain any root resource classes"));
        }
    }
    
    @Test
    public void confirmNoResourceLoadedFromGuiceBinding() throws InterruptedException, MalformedURLException, IOException {
        try (LifecycleInjector injector = InjectorBuilder.fromModules(
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
                        .createInjector()) {
            Assert.fail();
        } catch (Exception e) {
            Assert.assertTrue(e.getCause().getMessage().contains("The ResourceConfig instance does not contain any root resource classes"));
        }
    }

    @Path("/")
    @Singleton
    public static class FieldInjectionResource {
        private static int createCount = 0;
        
        public FieldInjectionResource() {
            createCount++;
        }
        
        @Inject
        private String foo;
        
        @Context
        UriInfo uri;
        
        @GET
        public String get() {
            return foo;
        }
    }
    
    @Test
    public void confirmNonJerseySingletonNotEagerOrASingleton() {
        // Create the injector and autostart Jetty
        try (LifecycleInjector injector = InjectorBuilder.fromModules(
                new ShutdownHookModule(), 
                new GovernatorJerseySupportModule(),
                new JerseyServletModule() {
                    protected void configureServlets() {
                        serve("/*").with(GovernatorServletContainer.class);
                        
                        bind(String.class).toInstance("foo");
                    }
                    
                    @Advises
                    @Singleton
                    @Named("governator")
                    UnaryOperator<DefaultResourceConfig> getResourceConfig() {
                        return config -> {
                            config.getClasses().add(FieldInjectionResource.class);
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
            
            Assert.assertEquals(0, FieldInjectionResource.createCount);
            
            // Determine the emphermal port from jetty
            Server server = injector.getInstance(Server.class);
            int port = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
            
            System.out.println("Listening on port : "+ port);
            
            URL url = new URL(String.format("http://localhost:%d/", port));
            {
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                Assert.assertEquals(200,  conn.getResponseCode());
                
                Assert.assertEquals(1, FieldInjectionResource.createCount);
            }
            
            {
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                Assert.assertEquals(200,  conn.getResponseCode());
                
                Assert.assertEquals(2, FieldInjectionResource.createCount);
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }
    
    @Path("/")
    @com.sun.jersey.spi.resource.Singleton
    public static class UriContextIsRequestScoped {
        @Context
        UriInfo uri;
        
        @GET
        @Path("{subResources:.*}")
        public String get(@PathParam("subResources") String subResources) {
            Assert.assertTrue(Proxy.isProxyClass(uri.getClass()));
            return uri.getPath();
        }
    }
    
    @Test
    public void confirmUriInfoContextIsRequestScoped() {
        // Create the injector and autostart Jetty
        try (LifecycleInjector injector = InjectorBuilder.fromModules(
                new ShutdownHookModule(), 
                new GovernatorJerseySupportModule(),
                new JerseyServletModule() {
                    protected void configureServlets() {
                        serve("/*").with(GovernatorServletContainer.class);
                        
                        bind(String.class).toInstance("foo");
                    }
                    
                    @Advises
                    @Singleton
                    @Named("governator")
                    UnaryOperator<DefaultResourceConfig> getResourceConfig() {
                        return config -> {
                            config.getClasses().add(UriContextIsRequestScoped.class);
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
            
            {
                URL url = new URL(String.format("http://localhost:%d/path1", port));
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                Assert.assertEquals(200,  conn.getResponseCode());
                Assert.assertEquals("path1", CharStreams.toString(new InputStreamReader(conn.getInputStream())));
            }
            
            {
                URL url = new URL(String.format("http://localhost:%d/path2", port));
                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                Assert.assertEquals(200,  conn.getResponseCode());
                Assert.assertEquals("path2", CharStreams.toString(new InputStreamReader(conn.getInputStream())));
            }
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail(e.getMessage());
        }
    }

}
