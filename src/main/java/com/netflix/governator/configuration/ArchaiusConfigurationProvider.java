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

import com.google.common.collect.Maps;
import com.netflix.config.ConfigurationManager;
import java.util.Map;

/**
 * Configuration provider backed by Netflix Archaius (https://github.com/Netflix/archaius)
 */
public class ArchaiusConfigurationProvider implements ConfigurationProvider
{
    private final Map<String, String> variableValues;

    public ArchaiusConfigurationProvider()
    {
        variableValues = Maps.newHashMap();
    }

    public ArchaiusConfigurationProvider(Map<String, String> variableValues)
    {
        this.variableValues = Maps.newHashMap(variableValues);
    }

    /**
     * Change a variable value
     *
     * @param name name
     * @param value value
     */
    public void     setVariable(String name, String value)
    {
        variableValues.put(name, value);
    }

    @Override
    public boolean has(ConfigurationKey key)
    {
        return ConfigurationManager.getConfigInstance().containsKey(key.getKey(variableValues));
    }

    @Override
    public boolean getBoolean(ConfigurationKey key)
    {
        return ConfigurationManager.getConfigInstance().getBoolean(key.getKey(variableValues));
    }

    @Override
    public int getInteger(ConfigurationKey key)
    {
        return ConfigurationManager.getConfigInstance().getInt(key.getKey(variableValues));
    }

    @Override
    public long getLong(ConfigurationKey key)
    {
        return ConfigurationManager.getConfigInstance().getLong(key.getKey(variableValues));
    }

    @Override
    public double getDouble(ConfigurationKey key)
    {
        return ConfigurationManager.getConfigInstance().getDouble(key.getKey(variableValues));
    }

    @Override
    public String getString(ConfigurationKey key)
    {
        return ConfigurationManager.getConfigInstance().getString(key.getKey(variableValues));
    }
}
