package com.netflix.governator.guice.jersey;

import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Injector;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.WebApplication;
import com.sun.jersey.spi.container.servlet.WebConfig;

/**
 * @see GovernatorJerseySupportModule
 */
@Singleton
public class GovernatorServletContainer extends GuiceContainer {
    private static final long serialVersionUID = -1350697205980976818L;
    private static final Logger LOG = LoggerFactory.getLogger(GovernatorServletContainer.class);
    
    private final ResourceConfig resourceConfig;
    private final Injector injector;

    private WebApplication webapp;
    
    @Inject
    protected GovernatorServletContainer(Injector injector, @Named("governator") ResourceConfig resourceConfig) {
        super(injector);
        this.resourceConfig = resourceConfig;
        this.injector = injector;
    }

    @Override
    protected ResourceConfig getDefaultResourceConfig(
            Map<String, Object> props,
            WebConfig webConfig) throws ServletException {
        if (!props.isEmpty()) {
            throw new IllegalArgumentException("Passing properties via serve() is no longer supported.  ResourceConfig properties should be set by the binding for ResourceConfig");
        }
        return this.resourceConfig;
    }
    
    @Override
    protected void initiate(ResourceConfig config, WebApplication webapp) {
        this.webapp = webapp;
        
        GovernatorComponentProviderFactory factory = new GovernatorComponentProviderFactory(config, injector);
        webapp.initiate(config, factory);
        
        // Make sure all root resources are created eagerly so they're fully initialized 
        // by the time the injector was created, instead of being created lazily at the
        // first request.
        for (Class<?> resource : config.getRootResourceClasses()) {
        	if (resource.isAnnotationPresent(com.google.inject.Singleton.class) 
    			|| resource.isAnnotationPresent(javax.inject.Singleton.class)) {
        	    LOG.warn("Class {} should be annotated with Jersey's com.sun.jersey.spi.resource.Singleton.  Also make sure that any JAX-RS clasese (such as UriInfo) are injected using Jersey's @Context instead of @Inject.", resource);
        	}
        }
    }

    public WebApplication getWebApplication() {
        return webapp;
    }

}
