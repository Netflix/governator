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
    boolean     has(String name);

    /**
     * Return the given configuration as a boolean
     *
     * @param name configuration name
     * @return value
     */
    boolean     getBoolean(String name);

    /**
     * Return the given configuration as an integer
     *
     * @param name configuration name
     * @return value
     */
    int         getInteger(String name);

    /**
     * Return the given configuration as a long
     *
     * @param name configuration name
     * @return value
     */
    long        getLong(String name);

    /**
     * Return the given configuration as a double
     *
     * @param name configuration name
     * @return value
     */
    double      getDouble(String name);

    /**
     * Return the given configuration as a string
     *
     * @param name configuration name
     * @return value
     */
    String      getString(String name);
}
