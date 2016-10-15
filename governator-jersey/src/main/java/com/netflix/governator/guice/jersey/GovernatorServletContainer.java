package com.netflix.governator.guice.jersey;

import com.google.inject.Injector;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.servlet.ServletContainer;
import com.sun.jersey.spi.container.servlet.WebConfig;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;

/**
 * @see GovernatorJerseySupportModule
 */
@Singleton
public class GovernatorServletContainer extends ServletContainer {
    private static final long serialVersionUID = -1350697205980976818L;

    private final ResourceConfig resourceConfig;
    private final Injector injector;

    private WebApplication webapp;
    
    @Inject
    protected GovernatorServletContainer(Injector injector, @Named("governator") ResourceConfig resourceConfig) {
        this.resourceConfig = resourceConfig;
        this.injector = injector;
    }

    @Override
    protected ResourceConfig getDefaultResourceConfig(
            Map<String, Object> props,
            WebConfig webConfig) throws ServletException {
        return this.resourceConfig;
    }
    
    @Override
    protected void initiate(ResourceConfig config, WebApplication webapp) {
        this.webapp = webapp;
        webapp.initiate(config, new GovernatorComponentProviderFactory(config, injector));
    }

    public WebApplication getWebApplication() {
        return webapp;
    }

}
