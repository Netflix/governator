package com.netflix.governator.guice.jetty;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.util.Modules;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjector;
import com.netflix.governator.ShutdownHookModule;
import com.netflix.governator.guice.jetty.resources1.SampleResource;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.PreDestroy;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.security.KeyStore;

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

    @Test
    public void testConnectorBinding() throws Exception {
        LifecycleInjector injector = InjectorBuilder.fromModules(
                new SampleServletModule(),
                new ShutdownHookModule(),
                Modules.override(new JettyModule())
                        .with(new AbstractModule() {
                            @Override
                            protected void configure() {
                            }

                            @Provides
                            JettyConfig getConfig() {
                                // Use ephemeral ports
                                return new DefaultJettyConfig().setPort(0);
                            }
                        }),
                new JettySslModule()
                ).createInjector();

        Server server = injector.getInstance(Server.class);
        Assert.assertEquals(2, server.getConnectors().length);
        KeyStore keyStore = injector.getInstance(KeyStore.class);

        int port = ((ServerConnector)server.getConnectors()[0]).getLocalPort();
        int sslPort = ((ServerConnector)server.getConnectors()[1]).getLocalPort();

        // Do a plaintext GET and verify that the default connector works
        String response = doGet(String.format("http://localhost:%d/", port), null);
        Assert.assertTrue(response.startsWith("hello "));

        // Do an SSL GET and verify the response is valid
        response = doGet(String.format("https://localhost:%d/", sslPort), keyStore);
        Assert.assertTrue(response.startsWith("hello "));

        injector.close();
    }

    private static String doGet(String url, KeyStore sslTrustStore) throws Exception {

        URLConnection urlConnection = new URL(url).openConnection();
        if (sslTrustStore != null) {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(sslTrustStore);
            sslContext.init(null, tmf.getTrustManagers(), null);

            HttpsURLConnection httpsURLConnection = (HttpsURLConnection) urlConnection;
            httpsURLConnection.setSSLSocketFactory(sslContext.getSocketFactory());
        }

        try (InputStream inputStream = urlConnection.getInputStream()) {
            byte[] buffer = new byte[4096];
            int n = inputStream.read(buffer);
            return new String(buffer, 0, n, StandardCharsets.UTF_8);
        }

    }
}
