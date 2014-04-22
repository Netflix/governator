package com.netflix.governator.lifecycle;

import com.google.common.collect.Maps;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.ConfigurationVariable;
import com.netflix.governator.configuration.ConfigurationDocumentation;
import com.netflix.governator.configuration.ConfigurationMapper;
import com.netflix.governator.configuration.ConfigurationProvider;

import java.lang.reflect.Field;
import java.util.Map;

public class DefaultConfigurationMapper implements ConfigurationMapper {
    @Override
    public void mapConfiguration(
            ConfigurationProvider configurationProvider, 
            ConfigurationDocumentation configurationDocumentation, 
            Object obj, 
            LifecycleMethods methods) throws Exception {
        
        /**
         * Any field annotated with @ConfigurationVariable will be available for
         * replacement when generating property names
         */
        Map<String, String> overrides = Maps.newHashMap();
        for ( Field variableField : methods.fieldsFor(ConfigurationVariable.class)) {
            ConfigurationVariable annot = variableField.getAnnotation(ConfigurationVariable.class);
            if (annot != null) {
                overrides.put(annot.name(), variableField.get(obj).toString());
            }
        }
        
        /**
         * Map a configuration to any field with @Configuration annotation
         */
        ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(configurationProvider, configurationDocumentation);
        for ( Field configurationField : methods.fieldsFor(Configuration.class) )
        {
            configurationProcessor.assignConfiguration(obj, configurationField, overrides);
        }
    }

}
