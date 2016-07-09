package com.netflix.governator.lifecycle;

import com.google.common.collect.Maps;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.ConfigurationVariable;
import com.netflix.governator.configuration.ConfigurationDocumentation;
import com.netflix.governator.configuration.ConfigurationMapper;
import com.netflix.governator.configuration.ConfigurationProvider;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
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
                Lookup lookup = MethodHandles.lookup();
                overrides = Maps.newHashMap();
                for ( Field variableField : configurationVariableFields) {
                    ConfigurationVariable annot = variableField.getAnnotation(ConfigurationVariable.class);
                    if (annot != null) {
                        overrides.put(annot.name(), invoke(variableField, obj).toString());
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

    private Object invoke(Field variableField, Object obj) throws InvocationTargetException, IllegalAccessException {
        MethodHandle handler = MethodHandles.lookup().unreflectGetter(variableField);
        try {
            return handler.invoke(obj);
        } catch (Throwable e) {
            throw new InvocationTargetException(e);
        }
    }

}
