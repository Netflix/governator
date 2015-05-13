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

import com.google.common.base.Supplier;
import com.google.common.collect.Maps;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Date;
import java.util.Map;

/**
 * ConfigurationProvider backed by the system properties
 */
public class SystemConfigurationProvider extends AbstractObjectConfigurationProvider
{
    private final Map<String, String> variableValues;

    public SystemConfigurationProvider()
    {
        this(Maps.<String, String>newHashMap());
    }

    public SystemConfigurationProvider(Map<String, String> variableValues)
    {
        this(variableValues, null);
    }

    public SystemConfigurationProvider(Map<String, String> variableValues, ObjectMapper objectMapper)
    {
        super(objectMapper);
        this.variableValues = Maps.newHashMap(variableValues);
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
        return System.getProperty(key.getKey(variableValues), null) != null;
    }

    @Override
    public Property<Boolean> getBooleanProperty(final ConfigurationKey key, final Boolean defaultValue)
    {
        return new Property<Boolean>()
        {
            @Override
            public Boolean get()
            {
                String value = System.getProperty(key.getKey(variableValues));
                if ( value == null )
                {
                    return defaultValue;
                }
                return Boolean.parseBoolean(value);
            }
        };
    }

    @Override
    public Property<Integer> getIntegerProperty(final ConfigurationKey key, final Integer defaultValue)
    {
        return new Property<Integer>()
        {
            @Override
            public Integer get()
            {
                Integer value;
                try {
                    value = Integer.parseInt(System.getProperty(key.getKey(variableValues)));
                } catch (NumberFormatException ex) {
                    return defaultValue;
                }
                return value;
            }
        };
    }

    @Override
    public Property<Long> getLongProperty(final ConfigurationKey key, final Long defaultValue)
    {
        return new Property<Long>()
        {
            @Override
            public Long get()
            {
                Long value;
                try {
                    value = Long.parseLong(System.getProperty(key.getKey(variableValues)));
                } catch (NumberFormatException ex) {
                    return defaultValue;
                }
                return value;
            }
        };
    }

    @Override
    public Property<Double> getDoubleProperty(final ConfigurationKey key, final Double defaultValue)
    {
        return new Property<Double>()
        {
            @Override
            public Double get()
            {
                Double value;
                try {
                    value = Double.parseDouble(System.getProperty(key.getKey(variableValues)));
                } catch (NumberFormatException ex) {
                    return defaultValue;
                }
                return value;
            }
        };
    }

    @Override
    public Property<String> getStringProperty(final ConfigurationKey key, final String defaultValue)
    {
        return new Property<String>()
        {
            @Override
            public String get()
            {
                String value = System.getProperty(key.getKey(variableValues));
                if ( value == null )
                {
                    return defaultValue;
                }
                return value;
            }
        };
    }

    @Override
    public Property<Date> getDateProperty(ConfigurationKey key, Date defaultValue)
    {
        return new DateWithDefaultProperty(getStringProperty(key, null), defaultValue);
    }

}
