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
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;
import com.sun.jersey.spi.container.servlet.WebConfig;

/**
 * Enhancement to {@link GuiceContainer} by allowing Jersey to be configured via bindings instead of 
 * statically in a {@link JerseyServletModule}.  In addition all singleton resources will be 
 * instantiated eagerly.
 * 
 * <pre>
 * {@code
 * bind(GuiceContainer.class).to(GovernatorServletContainer.class).asEagerSingleton();
 * }
 * </pre>
 * 
 * To set up a Jersey endpoint,
 *
 * <pre>
 * {@code
 * serve("/*").with(GuiceContainer.class);
 * }
 * </pre>
 * 
 * To customize the configuration of the Jersey endpoint
 *
 * <pre>
 * {@code
 * @Provides
 * ResourceConfig getResourceConfig() {
 *     return new PackagesResourceConfig(ImmutableMap.<String, Object>builder()
 *             .put(PackagesResourceConfig.PROPERTY_PACKAGES, "org.example.resources")
 *             .put(ResourceConfig.FEATURE_DISABLE_WADL, "false")   // Enable WADL
 *             .build());
 * }
 * }
 * </pre>
 */
@Singleton
public final class GovernatorServletContainer extends GuiceContainer {
    private static final long serialVersionUID = -1350697205980976818L;

    private static final Logger LOG = LoggerFactory.getLogger(GovernatorServletContainer.class);
    
    private final Injector injector;
    private ResourceConfig args;
    
    static class Arguments {
        @com.google.inject.Inject(optional=true)
        ResourceConfig config;
    }
    
    @Inject
    GovernatorServletContainer(Injector injector, Arguments args) {
        super(injector);
        
        this.injector = injector;
        this.args = args.config;
    }

    @Override
    protected ResourceConfig getDefaultResourceConfig(
            Map<String, Object> props,
            WebConfig webConfig) throws ServletException {
        
        if (args == null) {
            args = super.getDefaultResourceConfig(props, webConfig);
        }
        
        for (Class<?> resource : args.getRootResourceClasses()) {
            if (Scopes.isSingleton(injector.getBinding(resource))) {
                LOG.info("Creating resource '{}'.", resource.getName());
                injector.getInstance(resource);
            }
            else {
                LOG.info("Resource '{}' will be created on first request.  Mark as @Singleton to create eagerly.", resource.getName());
            }
        }
        return args;
    }
}
