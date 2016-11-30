package com.netflix.governator;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Provider;
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
    
    private static class Mapper {
        private ConfigurationMapper mapper;
        private ConfigurationProvider configurationProvider;
        private ConfigurationDocumentation configurationDocumentation;
        
        @Inject
        Mapper(ConfigurationMapper mapper, 
              ConfigurationProvider configurationProvider, 
              ConfigurationDocumentation configurationDocumentation) {
            this.mapper = mapper;
            this.configurationProvider = configurationProvider;
            this.configurationDocumentation = configurationDocumentation;
        }
        
        private void mapConfiguration(Object obj, LifecycleMethods methods) throws Exception {
            mapper.mapConfiguration(configurationProvider, configurationDocumentation, obj, methods);
        }
    }
    
    private volatile Provider<Mapper> mapper;
    
    @Inject
    public void initialize(Provider<Mapper> state) {
        this.mapper = state;
    }
    
    @Override
    public List<LifecycleAction> getActionsForType(final Class<?> type) {
        final LifecycleMethods methods = new LifecycleMethods(type);
        if (methods.annotatedFields(Configuration.class).length > 0) {
            return Arrays.<LifecycleAction>asList(new LifecycleAction() {
                @Override
                public void call(Object obj) throws Exception {
                    if (mapper == null) {
                        throw new ProvisionException("Trying to map fields of type " + type.getName() + " before ConfigurationLifecycleFeature was fully initialized by the injector");
                    }
                    
                    try {
                        mapper.get().mapConfiguration(obj, methods);
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
        return "ConfigurationLifecycleFeature[]";
    }

}
