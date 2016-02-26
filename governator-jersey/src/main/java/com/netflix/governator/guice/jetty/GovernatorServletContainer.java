package com.netflix.governator.guice.jetty;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.servlet.WebConfig;

/**
 * @see GovernatorJerseyServletModule
 */
@Singleton
final class GovernatorServletContainer extends GuiceContainer {
    private static final long serialVersionUID = -1350697205980976818L;

    private static final Logger LOG = LoggerFactory.getLogger(GovernatorServletContainer.class);
    
    private final Injector injector;
    private ResourceConfig config;
    
    static class Arguments {
        @com.google.inject.Inject(optional=true)
        ResourceConfig config;
    }
    
    @Inject
    GovernatorServletContainer(Injector injector, Arguments config) {
        super(injector);
        
        this.injector = injector;
        this.config = config.config;
    }

    @Override
    protected ResourceConfig getDefaultResourceConfig(
            Map<String, Object> props,
            WebConfig webConfig) throws ServletException {
        
        if (config == null) {
            config = super.getDefaultResourceConfig(props, webConfig);
        }
        
        for (Class<?> resource : config.getRootResourceClasses()) {
            if (Scopes.isSingleton(injector.getBinding(resource))) {
                LOG.info("Creating resource '{}'.", resource.getName());
                injector.getInstance(resource);
            }
            else {
                LOG.info("Resource '{}' will be created on first request.  Mark as @Singleton to create eagerly.", resource.getName());
            }
        }
        return config;
    }

}
