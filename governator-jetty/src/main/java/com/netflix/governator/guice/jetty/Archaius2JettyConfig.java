package com.netflix.governator.guice.jetty;

import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;

@Configuration(prefix="governator.jetty.embedded")
public interface Archaius2JettyConfig extends JettyConfig {
    
    @DefaultValue("8080")
    int getPort();

    /**
     * @deprecated 2016-10-14 use {@link #getStaticResourceBase()} instead
     */
    @Deprecated
    @DefaultValue("/META-INF/resources/")
    String getResourceBase();

    /**
     * @return The directory where the webapp has the static resources. It can just be a suffix since we'll scan the
     * classpath to find the exact directory name.
     */
    @DefaultValue("/META-INF/resources/")
    String getStaticResourceBase();

    /**
     * @return web app base resource path
     */
    @DefaultValue("src/main/webapp")
    String getWebAppResourceBase();

    /**
     * @return the default web app context path for jetty
     */
    @DefaultValue("/")
    String getWebAppContextPath();
}
