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
