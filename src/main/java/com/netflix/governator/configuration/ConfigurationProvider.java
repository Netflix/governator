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
