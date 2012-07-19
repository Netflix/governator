package com.netflix.governator.configuration;

public interface ConfigurationProvider
{
    boolean     has(String name);

    boolean     getBoolean(String name);

    int         getInteger(String name);

    long        getLong(String name);

    double      getDouble(String name);

    String      getString(String name);
}
