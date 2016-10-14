package com.netflix.governator.guice.jetty;

public interface JettyConfig {

    int getPort();

    /**
     * @deprecated 2016-10-14 use {@link #getStaticResourceBase()} instead
     *
     * @return The directory where the webapp has the static resources. It can just be a suffix since we'll scan the
     * classpath to find the exact directory name.
     */
    @Deprecated
    default String getResourceBase() { return getStaticResourceBase(); };

    /**
     * @return The directory where the webapp has the static resources. It can just be a suffix since we'll scan the
     * classpath to find the exact directory name.
     */
    String getStaticResourceBase();

    /**
     * @return the web app resource base
     */
    String getWebAppResourceBase();

    /**
     * @return the web app context path
     */
    default String getWebAppContextPath() { return "/"; }
    
}
