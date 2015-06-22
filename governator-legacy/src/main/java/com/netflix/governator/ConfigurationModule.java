package com.netflix.governator;

import com.google.inject.AbstractModule;
import com.google.inject.multibindings.Multibinder;
import com.netflix.governator.configuration.ConfigurationProvider;

/**
 * Install this module to enable @Configuration and @ConfigurationParameter 
 * annotation processing.
 * 
 * @author elandau
 *
 */
public class ConfigurationModule extends AbstractModule {
    @Override
    protected void configure() {
        Multibinder.newSetBinder(binder(), LifecycleFeature.class).addBinding().to(ConfigurationLifecycleFeature.class);
        requireBinding(ConfigurationProvider.class);
    }
}
