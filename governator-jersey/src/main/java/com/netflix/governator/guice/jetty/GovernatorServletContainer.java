package com.netflix.governator.guice.jetty;

import java.util.Collections;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableMap;
import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.netflix.governator.jersey.annotations.ServletContainerProperties;
import com.sun.jersey.api.core.PackagesResourceConfig;
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
    
    private final Map<String, Object> props;
    private final Injector injector;

    final static class Configuration {
        @com.google.inject.Inject(optional=true)
        @ServletContainerProperties
        Map<String, Object> props;
        
        Map<String, Object> getProperties() {
            return Objects.firstNonNull(props, Collections.<String, Object>emptyMap());
        }
    }
    
    @Inject
    GovernatorServletContainer(Injector injector, Configuration config) {
        super(injector);
        
        this.injector = injector;
        this.props = config.getProperties();
    }

    @Override
    protected ResourceConfig getDefaultResourceConfig(
            Map<String, Object> props,
            WebConfig webConfig) throws ServletException {
        
        Map<String, Object> newProps = ImmutableMap.<String, Object>builder()
                .put(ResourceConfig.FEATURE_DISABLE_WADL, "false")
                .putAll(props)
                .putAll(this.props)
                .build();
        
        final ResourceConfig config;
        if (newProps.containsKey(PackagesResourceConfig.PROPERTY_PACKAGES)) {
            config = new PackagesResourceConfig(newProps);
        }
        else {
            config = super.getDefaultResourceConfig(newProps, webConfig);
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
