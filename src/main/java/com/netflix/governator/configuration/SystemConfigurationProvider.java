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
import java.util.Date;
import java.util.Map;

/**
 * ConfigurationProvider backed by the system properties
 */
public class SystemConfigurationProvider implements ConfigurationProvider
{
    private final Map<String, String> variableValues;

    public SystemConfigurationProvider()
    {
        this(Maps.<String, String>newHashMap());
    }

    public SystemConfigurationProvider(Map<String, String> variableValues)
    {
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
    public Supplier<Boolean> getBooleanSupplier(final ConfigurationKey key, final Boolean defaultValue)
    {
        return new Supplier<Boolean>()
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
    public Supplier<Integer> getIntegerSupplier(final ConfigurationKey key, final Integer defaultValue)
    {
        return new Supplier<Integer>()
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
    public Supplier<Long> getLongSupplier(final ConfigurationKey key, final Long defaultValue)
    {
        return new Supplier<Long>()
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
    public Supplier<Double> getDoubleSupplier(final ConfigurationKey key, final Double defaultValue)
    {
        return new Supplier<Double>()
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
    public Supplier<String> getStringSupplier(final ConfigurationKey key, final String defaultValue)
    {
        return new Supplier<String>()
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
    public Supplier<Date> getDateSupplier(ConfigurationKey key, Date defaultValue)
    {
        return new DateWithDefaultSupplier(getStringSupplier(key, null), defaultValue);
    }

}
