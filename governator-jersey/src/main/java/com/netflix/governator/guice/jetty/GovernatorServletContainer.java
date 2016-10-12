package com.netflix.governator.guice.jetty;

import com.google.inject.Injector;
import com.netflix.governator.guice.jersey.GovernatorJerseySupportModule;
import com.sun.jersey.api.core.ResourceConfig;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

/**
 * @see GovernatorJerseySupportModule
 */
@Singleton
@Deprecated
public final class GovernatorServletContainer extends com.netflix.governator.guice.jersey.GovernatorServletContainer {
    private static final long serialVersionUID = -1350697205980976818L;

    @Inject
    GovernatorServletContainer(Injector injector, @Named("governator") ResourceConfig resourceConfig) {
        super(injector, resourceConfig);
    }
}
