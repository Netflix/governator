package com.netflix.governator.configuration;

public class SystemConfigurationProvider implements ConfigurationProvider
{
    @Override
    public boolean has(String name)
    {
        return System.getProperty(name, null) != null;
    }

    @Override
    public boolean getBoolean(String name)
    {
        return Boolean.getBoolean(name);
    }

    @Override
    public int getInteger(String name)
    {
        return Integer.getInteger(name);
    }

    @Override
    public long getLong(String name)
    {
        return Long.getLong(name);
    }

    @Override
    public double getDouble(String name)
    {
        return Double.parseDouble(System.getProperty(name));
    }

    @Override
    public String getString(String name)
    {
        return System.getProperty(name);
    }
}
