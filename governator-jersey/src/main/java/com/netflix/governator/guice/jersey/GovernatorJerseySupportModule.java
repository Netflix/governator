package com.netflix.governator.guice.jersey;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.governator.providers.AdvisableAnnotatedMethodScanner;
import com.netflix.governator.providers.ProvidesWithAdvice;
import com.sun.jersey.api.core.DefaultResourceConfig;
import com.sun.jersey.api.core.ResourceConfig;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.servlet.ServletContext;

/**
 * Module enabling support for Jersey configuration via advisable bindings for {@link DefaultResourceConfig}.
 * This module provides an alternative Guice integration based on jersey-guice {@link GuiceContainer} but with 
 * less opinions, such as automatically adding any bound class with {@literal @}Path as a resources.
 * 
 * Applications are expected to customize {@link DefaultResourceConfig} to provide a whitelist of
 * root resources to expose.  This can be done by adding the following {@link @Advises} method to any
 * Guice module (normally an implementation of JerseyServletModule)
 * 
 * <pre>{@code
@Advises
@Singleton
@Named("governator")
UnaryOperator<DefaultResourceConfig> adviseWithMyApplicationResources() {
    return defaultResourceConfig -> {
        // All customizations on {@link DefaultResourceConfig} will be picked up
        
        // Specify list of @Path annotated resourcesto serve
        defaultResourceConfig.getClasses().addAll(Arrays.asList(
            MyApplicationResource.class
        ));
        return defaultResourceConfig;
    };
}
 * }</pre>
 * 
 * Additionally this module adds a binding to the {@link ServletContext} for Jersey.  It can be injected 
 * as {@code @Named("governator") ServletContext}
 * 
 * @see GovernatorServletContainer
 */
public final class GovernatorJerseySupportModule extends AbstractModule {
    @Override
    protected void configure() {
        bind(GuiceContainer.class).to(GovernatorServletContainer.class).asEagerSingleton();
        install(AdvisableAnnotatedMethodScanner.asModule());
    }
    
    @ProvidesWithAdvice
    @Singleton
    @Named("governator")
    DefaultResourceConfig getDefaultResourceConfigConfig() {
        return new DefaultResourceConfig();
    }
    
    @Singleton
    @Provides
    @Named("governator")
    ResourceConfig getResourceConfig(@Named("governator") DefaultResourceConfig config) {
        return config;
    }
    
    @Singleton
    @Provides
    @Named("governator")
    ServletContext getServletContext(GovernatorServletContainer container) {
        return container.getServletContext();
    }
    
    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return getClass().equals(obj.getClass());
    }
}
