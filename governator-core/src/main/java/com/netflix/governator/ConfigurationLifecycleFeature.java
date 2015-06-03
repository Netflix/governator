package com.netflix.governator;

import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.ProvisionException;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.configuration.ConfigurationDocumentation;
import com.netflix.governator.configuration.ConfigurationMapper;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.lifecycle.LifecycleMethods;


/**
 * Feature to enable @Configuration annotation processing.
 * 
 * To enable install the ConfigurationModule.
 * 
 * <pre>
 * {@code
 * install(new ConfigurationModule());
 * }
 * </pre>
 * @author elandau
 *
 */
@Singleton
public class ConfigurationLifecycleFeature implements LifecycleFeature {
    
    private ConfigurationMapper mapper;
    private ConfigurationProvider configurationProvider;
    private ConfigurationDocumentation configurationDocumentation;

    @Inject
    public void initialize(
            ConfigurationMapper mapper, 
            ConfigurationProvider configurationProvider, 
            ConfigurationDocumentation configurationDocumentation
            ) {
        this.mapper = mapper;
        this.configurationDocumentation = configurationDocumentation;
        this.configurationProvider = configurationProvider;
    }
    
    @Override
    public List<LifecycleAction> getActionsForType(final Class<?> type) {
        final LifecycleMethods methods = new LifecycleMethods(type);
        if (!methods.fieldsFor(Configuration.class).isEmpty()) {
            return Arrays.<LifecycleAction>asList(new LifecycleAction() {
                @Override
                public void call(Object obj) throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {
                    if (mapper == null) {
                        throw new ProvisionException("Trying to map fields of type " + type.getName() + " before ConfigurationLifecycleFeature was fully initialized by the injector");
                    }
                    try {
                        mapper.mapConfiguration(configurationProvider, configurationDocumentation, obj, methods);
                    } catch (Exception e) {
                        throw new ProvisionException("Failed to map configuration for type " + type.getName(), e);
                    }
                }
            });
        }
        else {
            return Collections.emptyList();
        }
    }
    
    @Override
    public String toString() {
        return "Configuration";
    }

}
