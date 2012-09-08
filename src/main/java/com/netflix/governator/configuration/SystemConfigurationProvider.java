package com.netflix.governator.configuration;

import com.google.common.collect.Maps;
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
        return System.getProperty(key.getKey(variableValues), null) != null;
    }

    @Override
    public boolean getBoolean(ConfigurationKey key)
    {
        return Boolean.getBoolean(key.getKey(variableValues));
    }

    @Override
    public int getInteger(ConfigurationKey key)
    {
        return Integer.getInteger(key.getKey(variableValues));
    }

    @Override
    public long getLong(ConfigurationKey key)
    {
        return Long.getLong(key.getKey(variableValues));
    }

    @Override
    public double getDouble(ConfigurationKey key)
    {
        return Double.parseDouble(System.getProperty(key.getKey(variableValues)));
    }

    @Override
    public String getString(ConfigurationKey key)
    {
        return System.getProperty(key.getKey(variableValues));
    }
}
