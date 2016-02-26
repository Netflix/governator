package com.netflix.governator.guice.jetty;

import com.google.inject.servlet.ServletModule;
import com.sun.jersey.guice.JerseyServletModule;
import com.sun.jersey.guice.spi.container.servlet.GuiceContainer;

/**
 * Enhancement to JerseyServletModule by allowing the GuiceContainer and Jersey to be configured
 * via bindings instead of statically in a Module.
 * 
 * You can provide the configuration values to Jersey using the binding
 * 
 * ```java
 *  @Provides
 *  @ServletContainerProperties 
 *  Map<String, Object> getJerseyProperties() {
 *      return ...;
 *  }
 * ```
 * 
 * In addition installing this module will automatically turn on the WADL (which can be turned off
 * by setting the property, com.sun.jersey.config.feature.DisableWADL=true.  Also, when running
 * in Stage.DEVELOPMENT all scanning singleton resources will be eagerly created.
 * 
 */
public abstract class GovernatorJerseyServletModule extends JerseyServletModule {
    private static final class InternalGovernatorJerseyServletModule extends ServletModule {
        @Override
        protected void configureServlets() {
            bind(GuiceContainer.class).to(GovernatorServletContainer.class).asEagerSingleton();
            serve("/*").with(GuiceContainer.class);
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }
        
        @Override
        public boolean equals(Object o) {
            return getClass().equals(o.getClass());
        }
    }
    
    @Override
    final protected void configureServlets() {
        install(new InternalGovernatorJerseyServletModule());
        configureMoreServlets();
    }
    
    /**
     * Override this to configure your servlets.
     */
    protected abstract void configureMoreServlets();
}
