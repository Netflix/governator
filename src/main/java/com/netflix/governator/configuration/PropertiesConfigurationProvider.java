package com.netflix.governator.configuration;

import java.util.Properties;

/**
 * ConfigurationProvider backed by a {#link Properties}
 */
public class PropertiesConfigurationProvider implements ConfigurationProvider
{
    private final Properties properties;

    /**
     * @param properties the properties
     */
    public PropertiesConfigurationProvider(Properties properties)
    {
        this.properties = properties;
    }

    @Override
    public boolean has(String name)
    {
        return properties.containsKey(name);
    }

    @Override
    public boolean getBoolean(String name)
    {
        return Boolean.parseBoolean(properties.getProperty(name));
    }

    @Override
    public int getInteger(String name)
    {
        return Integer.parseInt(properties.getProperty(name));
    }

    @Override
    public long getLong(String name)
    {
        return Long.parseLong(properties.getProperty(name));
    }

    @Override
    public double getDouble(String name)
    {
        return Double.parseDouble(properties.getProperty(name));
    }

    @Override
    public String getString(String name)
    {
        return properties.getProperty(name);
    }
}
