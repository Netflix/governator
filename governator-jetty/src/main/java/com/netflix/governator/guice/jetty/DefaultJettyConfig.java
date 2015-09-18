package com.netflix.governator.guice.jetty;

import javax.inject.Singleton;

@Singleton
public class DefaultJettyConfig implements JettyConfig {
    private int port = 8080;
    
    @Override
    public int getPort() {
        return port;
    }
    
    public DefaultJettyConfig setPort(int port) {
        this.port = port;
        return this;
    }
}
