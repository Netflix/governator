package com.netflix.governator.lifecycle;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.Map;

import com.google.common.collect.Maps;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.ConfigurationVariable;
import com.netflix.governator.configuration.ConfigurationDocumentation;
import com.netflix.governator.configuration.ConfigurationMapper;
import com.netflix.governator.configuration.ConfigurationProvider;

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
        Field[] configurationFields = methods.annotatedFields(Configuration.class);
        if (configurationFields.length > 0) {
            /**
             * Any field annotated with @ConfigurationVariable will be available for replacement when generating
             * property names
             */
            final Map<String, String> overrides;
            Field[] configurationVariableFields = methods.annotatedFields(ConfigurationVariable.class);
            if (configurationVariableFields.length > 0) {
                overrides = Maps.newHashMap();
                for (Field variableField : configurationVariableFields) {
                    ConfigurationVariable annot = variableField.getAnnotation(ConfigurationVariable.class);
                    if (annot != null) {
                        overrides.put(annot.name(), methods.fieldGet(variableField, obj).toString());
                    }
                }
            }
            else {
                overrides = Collections.emptyMap();
            }

            ConfigurationProcessor configurationProcessor = new ConfigurationProcessor(configurationProvider,
                    configurationDocumentation);
            for (Field configurationField : configurationFields) {
                try {
                    configurationProcessor.assignConfiguration(obj, configurationField, overrides);
                } catch (Exception e) {
                    throw new Exception(String.format("Failed to bind property '%s' for instance of '%s'",
                            configurationField.getName(), obj.getClass().getCanonicalName()), e);
                }
            }
        }
    }

}
