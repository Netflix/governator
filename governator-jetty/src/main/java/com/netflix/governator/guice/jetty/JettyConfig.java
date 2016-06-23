package com.netflix.governator.guice.jetty;

public interface JettyConfig {

    int getPort();

    /**
     * @return The directory where the webapp has the static resources. It can just be a suffix since we'll scan the
     * classpath to find the exact directory name.
     */
    String getResourceBase();
    
}
