/*
 * Copyright 2013 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.governator.configuration;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.configuration.AbstractConfiguration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Maps;
import com.netflix.config.ConfigurationManager;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.config.PropertyWrapper;

/**
 * Configuration provider backed by Netflix Archaius (https://github.com/Netflix/archaius)
 */
public class ArchaiusConfigurationProvider extends AbstractObjectConfigurationProvider
{
    private final Map<String, String> variableValues;
    private final AbstractConfiguration configurationManager;
    private final DynamicPropertyFactory propertyFactory;
    private final ConfigurationOwnershipPolicy ownershipPolicy;

    public static class Builder
    {
        private Map<String, String> variableValues = Maps.newHashMap();
        private AbstractConfiguration configurationManager = ConfigurationManager.getConfigInstance();
        private DynamicPropertyFactory propertyFactory = DynamicPropertyFactory.getInstance();
        private ConfigurationOwnershipPolicy ownershipPolicy = ConfigurationOwnershipPolicies.ownsAll();
        private ObjectMapper objectMapper = new ObjectMapper();

        /**
         * Set of variables to use when expanding property key names
         */
        public Builder withVariableValues(Map<String, String> variableValues)
        {
            this.variableValues = variableValues;
            return this;
        }

        /**
         * Archaius configuration manager to use.  Defaults to ConfigurationManager.getConfigInstance()
         */
        public Builder withConfigurationManager(AbstractConfiguration configurationManager)
        {
            this.configurationManager = configurationManager;
            return this;
        }

        /**
         * Dynamic property factory to use for Supplier<?> attributes.  Defaults to DynamicPropertyFactory.getInstance()
         */
        public Builder withPropertyFactory(DynamicPropertyFactory propertyFactory)
        {
            this.propertyFactory = propertyFactory;
            return this;
        }

        /**
         * When set to true this configuration provider will 'have' all properties regardless of whether they
         * have been set yet or not.  This is very important for dynamic properties that have a default value
         * but haven't been overriden yet.
         */
        public Builder withOwnershipPolicy(ConfigurationOwnershipPolicy policy)
        {
            this.ownershipPolicy = policy;
            return this;
        }

        public Builder withObjectMapper(ObjectMapper objectMapper)
        {
            this.objectMapper = objectMapper;
            return this;
        }

        public ArchaiusConfigurationProvider build()
        {
            if ( this.ownershipPolicy == null )
            {
                this.ownershipPolicy = new ConfigurationOwnershipPolicy()
                {
                    @Override
                    public boolean has(ConfigurationKey key, Map<String, String> variables)
                    {
                        return configurationManager.containsKey(key.getKey(variableValues));
                    }
                };
            }
            return new ArchaiusConfigurationProvider(this);
        }

        private Builder()
        {
        }
    }

    public static Builder builder()
    {
        return new Builder();
    }

    /**
     * Adapter to convert Archaius internal PropertyWrapper to a standard Guava supplier
     *
     * @author elandau
     */
    public static class PropertyWrapperProperty<T> extends Property<T>
    {
        private final PropertyWrapper<T> wrapper;

        public PropertyWrapperProperty(PropertyWrapper<T> wrapper)
        {
            this.wrapper = wrapper;
        }

        @Override
        public T get()
        {
            return this.wrapper.getValue();
        }
    }

    @Deprecated
    public ArchaiusConfigurationProvider()
    {
        this(new HashMap<String, String>());
    }

    @Deprecated
    public ArchaiusConfigurationProvider(Map<String, String> variableValues)
    {
        this.variableValues = Maps.newHashMap(variableValues);
        this.configurationManager = ConfigurationManager.getConfigInstance();
        this.propertyFactory = DynamicPropertyFactory.getInstance();
        this.ownershipPolicy = new ConfigurationOwnershipPolicy()
        {
            @Override
            public boolean has(ConfigurationKey key, Map<String, String> variables)
            {
                return configurationManager.containsKey(key.getKey(variables));
            }
        };
    }

    private ArchaiusConfigurationProvider(Builder builder)
    {
        super(builder.objectMapper);
        this.variableValues = builder.variableValues;
        this.configurationManager = builder.configurationManager;
        this.propertyFactory = builder.propertyFactory;
        this.ownershipPolicy = builder.ownershipPolicy;
    }

    protected static Property<?> getDynamicProperty(Class<?> type, String key, String defaultValue, DynamicPropertyFactory propertyFactory)
    {
        if ( type.isAssignableFrom(String.class) )
        {
            return new PropertyWrapperProperty<String>(
                propertyFactory.getStringProperty(
                    key,
                    defaultValue));
        }
        else if ( type.isAssignableFrom(Integer.class) )
        {
            return new PropertyWrapperProperty<Integer>(
                propertyFactory.getIntProperty(
                    key,
                    defaultValue == null ? 0 : Integer.parseInt(defaultValue)));
        }
        else if ( type.isAssignableFrom(Double.class) )
        {
            return new PropertyWrapperProperty<Double>(
                propertyFactory.getDoubleProperty(
                    key,
                    defaultValue == null ? 0.0 : Double.parseDouble(defaultValue)));
        }
        else if ( type.isAssignableFrom(Long.class) )
        {
            return new PropertyWrapperProperty<Long>(
                propertyFactory.getLongProperty(
                    key,
                    defaultValue == null ? 0L : Long.parseLong(defaultValue)));
        }
        else if ( type.isAssignableFrom(Boolean.class) )
        {
            return new PropertyWrapperProperty<Boolean>(
                propertyFactory.getBooleanProperty(
                    key,
                    defaultValue == null ? false : Boolean.parseBoolean(defaultValue)));
        }
        throw new RuntimeException("Unsupported value type " + type.getCanonicalName());
    }

    /**
     * Change a variable value
     *
     * @param name  name
     * @param value value
     */
    public void setVariable(String name, String value)
    {
        variableValues.put(name, value);
    }

    @Override
    public boolean has(ConfigurationKey key)
    {
        return ownershipPolicy.has(key, variableValues);
    }

    @Override
    public Property<Boolean> getBooleanProperty(ConfigurationKey key, Boolean defaultValue)
    {
        return new PropertyWrapperProperty<Boolean>(
            propertyFactory.getBooleanProperty(
                key.getKey(variableValues),
                defaultValue));
    }

    @Override
    public Property<Integer> getIntegerProperty(ConfigurationKey key, Integer defaultValue)
    {
        return new PropertyWrapperProperty<Integer>(
            propertyFactory.getIntProperty(
                key.getKey(variableValues),
                defaultValue));
    }

    @Override
    public Property<Long> getLongProperty(ConfigurationKey key, Long defaultValue)
    {
        return new PropertyWrapperProperty<Long>(
            propertyFactory.getLongProperty(
                key.getKey(variableValues),
                defaultValue));
    }

    @Override
    public Property<Double> getDoubleProperty(ConfigurationKey key, Double defaultValue)
    {
        return new PropertyWrapperProperty<Double>(
            propertyFactory.getDoubleProperty(
                key.getKey(variableValues),
                defaultValue));
    }

    @Override
    public Property<String> getStringProperty(ConfigurationKey key, String defaultValue)
    {
        return new PropertyWrapperProperty<String>(
            propertyFactory.getStringProperty(
                key.getKey(variableValues),
                defaultValue));
    }

    @Override
    public Property<Date> getDateProperty(ConfigurationKey key, Date defaultValue)
    {
        return new DateWithDefaultProperty(getStringProperty(key, null), defaultValue);
    }
}
