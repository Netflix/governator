package com.netflix.governator.lifecycle;

import com.google.common.collect.Maps;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.ConfigurationVariable;
import com.netflix.governator.configuration.ConfigurationDocumentation;
import com.netflix.governator.configuration.ConfigurationMapper;
import com.netflix.governator.configuration.ConfigurationProvider;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class DefaultConfigurationMapper implements ConfigurationMapper {
    @Override
    public void mapConfiguration(
            ConfigurationProvider configurationProvider, 
            ConfigurationDocumentation configurationDocumentation, 
            Object obj, 
            LifecycleMethods methods) throws Exception {
        
        /**
         * Map a configuration to any field with @Configuration annotation
         */
        Collection<Field> configurationFields = methods.fieldsFor(Configuration.class);
        if (!configurationFields.isEmpty()) {
            /**
             * Any field annotated with @ConfigurationVariable will be available for
             * replacement when generating property names
             */
            final Map<String, String> overrides;
            Collection<Field> configurationVariableFields = methods.fieldsFor(ConfigurationVariable.class);
            if (!configurationVariableFields.isEmpty()) {
                overrides = Maps.newHashMap();
                for ( Field variableField : configurationVariableFields) {
                    ConfigurationVariable annot = variableField.getAnnotation(ConfigurationVariable.class);
                    if (annot != null) {
                        overrides.put(annot.name(), methods.fieldGet(variableField, obj).toString());
                    }
                }
            }
            else {
                overrides = Collections.emptyMap();
            }
            
            ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(configurationProvider, configurationDocumentation);
            for ( Field configurationField : configurationFields )
            {
                try {
                    configurationProcessor.assignConfiguration(obj, configurationField, overrides);
                }
                catch (Exception e) {
                    throw new Exception(String.format("Failed to bind property '%s' for instance of '%s'", configurationField.getName(), obj.getClass().getCanonicalName()), e);
                }
            }
        }
    }


}
