package com.netflix.governator.guice.jetty;

import com.netflix.archaius.api.annotations.Configuration;
import com.netflix.archaius.api.annotations.DefaultValue;

@Configuration(prefix="governator.jetty.embedded")
public interface Archaius2JettyConfig extends JettyConfig {
    
    @DefaultValue("8080")
    int getPort();

    /**
     * @return The directory where the webapp has the static resources. It can just be a suffix since we'll scan the
     * classpath to find the exact directory name.
     */
    @DefaultValue("/META-INF/resources/")
    String getResourceBase();

    /**
     * @return the default context path for jetty
     */
    @DefaultValue("/")
    String getContextPath();
}
