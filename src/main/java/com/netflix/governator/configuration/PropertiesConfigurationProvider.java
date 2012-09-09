package com.netflix.governator.configuration;

import com.google.common.collect.Maps;
import java.util.Map;
import java.util.Properties;

/**
 * ConfigurationProvider backed by a {#link Properties}
 */
public class PropertiesConfigurationProvider implements ConfigurationProvider
{
    private final Properties properties;
    private final Map<String, String> variableValues;

    /**
     * @param properties the properties
     */
    public PropertiesConfigurationProvider(Properties properties)
    {
        this(properties, Maps.<String, String>newHashMap());
    }

    public PropertiesConfigurationProvider(Properties properties, Map<String, String> variableValues)
    {
        this.properties = properties;
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
        return properties.containsKey(key.getKey(variableValues));
    }

    @Override
    public boolean getBoolean(ConfigurationKey key)
    {
        return Boolean.parseBoolean(properties.getProperty(key.getKey(variableValues)));
    }

    @Override
    public int getInteger(ConfigurationKey key)
    {
        return Integer.parseInt(properties.getProperty(key.getKey(variableValues)));
    }

    @Override
    public long getLong(ConfigurationKey key)
    {
        return Long.parseLong(properties.getProperty(key.getKey(variableValues)));
    }

    @Override
    public double getDouble(ConfigurationKey key)
    {
        return Double.parseDouble(properties.getProperty(key.getKey(variableValues)));
    }

    @Override
    public String getString(ConfigurationKey key)
    {
        return properties.getProperty(key.getKey(variableValues));
    }
}
