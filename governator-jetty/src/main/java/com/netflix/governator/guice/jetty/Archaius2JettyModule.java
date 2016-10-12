package com.netflix.governator.guice.jetty;

import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.archaius.ConfigProxyFactory;

public final class Archaius2JettyModule extends AbstractModule {

    @Override
    protected void configure() {
        install(new JettyModule());   
    }
    
    @Provides
    @Singleton
    public JettyConfig jettyConfig(ConfigProxyFactory configProxyFactory) {
        return configProxyFactory.newProxy(Archaius2JettyConfig.class);
    }
    
    @Override
    public boolean equals(Object obj) {
        return Archaius2JettyModule.class.equals(obj.getClass());
    }

    @Override
    public int hashCode() {
        return Archaius2JettyModule.class.hashCode();
    }

}
