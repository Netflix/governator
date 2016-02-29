package com.netflix.governator.guice.jetty;

import com.google.inject.servlet.ServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

public class SampleServletModule extends ServletModule {
    @Override
    protected void configureServlets() {
        bind(SampleResource.class);
        bind(GuiceContainer.class);        
        serve("/*").with(GuiceContainer.class);
    }
}
