package com.netflix.governator.guice.jetty;

import javax.inject.Singleton;

@Singleton
public class DefaultJettyConfig implements JettyConfig {
    private int port = 8080;

    // Where static files live. We pass this to Jetty for class path scanning to find the exact directory.
    // The default is to use resources supported by the servlet 3.0 spec.
    private String staticResourceBase = "/META-INF/resources/";

    private String webAppResourceBase = "src/main/webapp";

    private String webAppContextPath = "/";

    @Override
    public int getPort() {
        return port;
    }

    @Override
    public String getResourceBase() {
        return staticResourceBase;
    }

    @Override
    public String getStaticResourceBase() {
        return staticResourceBase;
    }

    @Override
    public String getWebAppResourceBase() {
        return webAppResourceBase;
    }

    @Override
    public String getWebAppContextPath() {
        return webAppContextPath;
    }

    public DefaultJettyConfig setPort(int port) {
        this.port = port;
        return this;
    }

    /**
     * @deprecated 2016-10-14 use {@link #setStaticResourceBase(String)} instead
     */
    @Deprecated
    public DefaultJettyConfig setResourceBase(String staticResourceBase) {
        this.staticResourceBase = staticResourceBase;
        return this;
    }

    public DefaultJettyConfig setStaticResourceBase(String staticResourceBase) {
        this.staticResourceBase = staticResourceBase;
        return this;
    }

    public DefaultJettyConfig setWebAppResourceBase(String webAppResourceBase) {
        this.webAppResourceBase = webAppResourceBase;
        return this;
    }

    public DefaultJettyConfig setWebAppContextPath(String webAppContextPath) {
        this.webAppContextPath = webAppContextPath;
        return this;
    }
}
