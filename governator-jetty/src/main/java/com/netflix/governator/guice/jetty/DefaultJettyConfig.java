package com.netflix.governator.guice.jetty;

import javax.inject.Singleton;

@Singleton
public class DefaultJettyConfig implements JettyConfig {
    private int port = 8080;

    // Where static files live. We pass this to Jetty for class path scanning to find the exact directory.
    // The default is to use resources supported by the servlet 3.0 spec.
    private String resourceBase = "/META-INF/resources/";

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getResourceBase() {
        return resourceBase;
    }

    public DefaultJettyConfig setPort(int port) {
        this.port = port;
        return this;
    }

    public DefaultJettyConfig setResourceBase(String resourceBase) {
        this.resourceBase = resourceBase;
        return this;
    }
}
