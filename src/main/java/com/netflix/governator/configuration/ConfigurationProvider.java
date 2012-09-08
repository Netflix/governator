package com.netflix.governator.configuration;

import com.netflix.governator.annotations.Configuration;

/**
 * Abstraction for get configuration values to use for fields annotated
 * with {@link Configuration}
 */
public interface ConfigurationProvider
{
    /**
     * Return true if there is a configuration value set for the given key
     *
     * @param key configuration key
     * @return true/false
     */
    public boolean     has(ConfigurationKey key);

    /**
     * Return the given configuration as a boolean
     *
     * @param key configuration key
     * @return value
     */
    public boolean     getBoolean(ConfigurationKey key);

    /**
     * Return the given configuration as an integer
     *
     * @param key configuration key
     * @return value
     */
    public int         getInteger(ConfigurationKey key);

    /**
     * Return the given configuration as a long
     *
     * @param key configuration key
     * @return value
     */
    public long        getLong(ConfigurationKey key);

    /**
     * Return the given configuration as a double
     *
     * @param key configuration key
     * @return value
     */
    public double      getDouble(ConfigurationKey key);

    /**
     * Return the given configuration as a string
     *
     * @param key configuration key
     * @return value
     */
    public String      getString(ConfigurationKey key);
}
