package com.netflix.governator.configuration;

import com.netflix.governator.annotations.Configuration;

/**
 * Abstraction for get configuration values to use for fields annotated
 * with {@link Configuration}
 */
public interface ConfigurationProvider
{
    /**
     * Return true if there is a configuration value set for the given name
     *
     * @param name configuration name
     * @return true/false
     */
    public boolean     has(String name);

    /**
     * Return the given configuration as a boolean
     *
     * @param name configuration name
     * @return value
     */
    public boolean     getBoolean(String name);

    /**
     * Return the given configuration as an integer
     *
     * @param name configuration name
     * @return value
     */
    public int         getInteger(String name);

    /**
     * Return the given configuration as a long
     *
     * @param name configuration name
     * @return value
     */
    public long        getLong(String name);

    /**
     * Return the given configuration as a double
     *
     * @param name configuration name
     * @return value
     */
    public double      getDouble(String name);

    /**
     * Return the given configuration as a string
     *
     * @param name configuration name
     * @return value
     */
    public String      getString(String name);
}
