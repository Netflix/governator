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

import com.google.common.base.Supplier;
import com.google.inject.ImplementedBy;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.lifecycle.LifecycleConfigurationProviders;

import java.util.Date;

/**
 * Abstraction for get configuration values to use for fields annotated
 * with {@link Configuration}
 */
@ImplementedBy(LifecycleConfigurationProviders.class)
public interface ConfigurationProvider
{
    /**
     * Return true if there is a configuration value set for the given key
     *
     * @param key configuration key
     * @return true/false
     */
    public boolean has(ConfigurationKey key);

    /**
     * Return the given configuration as a boolean.  Use this when the configuration
     * value is expected to change at run time.
     *
     * @param key configuration key
     * @return value
     */
    public Supplier<Boolean> getBooleanSupplier(ConfigurationKey key, Boolean defaultValue);

    /**
     * Return the given configuration as an integer.   Use this when the configuration
     * value is expected to change at run time.
     *
     * @param key configuration key
     * @return value
     */
    public Supplier<Integer> getIntegerSupplier(ConfigurationKey key, Integer defaultValue);

    /**
     * Return the given configuration as a long.  Use this when the configuration
     * value is expected to change at run time.
     *
     * @param key configuration key
     * @return value
     */
    public Supplier<Long> getLongSupplier(ConfigurationKey key, Long defaultValue);

    /**
     * Return the given configuration as a double.  Use this when the configuration
     * value is expected to change at run time.
     *
     * @param key configuration key
     * @return value
     */
    public Supplier<Double> getDoubleSupplier(ConfigurationKey key, Double defaultValue);

    /**
     * Return the given configuration as a string.  Use this when the configuration
     * value is expected to change at run time.
     *
     * @param key configuration key
     * @return value
     */
    public Supplier<String> getStringSupplier(ConfigurationKey key, String defaultValue);

    /**
     * Return the given configuration as a date.  Use this when the configuration
     * value is expected to change at run time.
     *
     * @param key configuration key
     * @return value
     */
    public Supplier<Date> getDateSupplier(ConfigurationKey key, Date defaultValue);

    /**
     * Return the given configuration as an object of the given type.
     *
     * @param key             configuration key
     * @param defaultValue    value to return when key is not found
     * @param objectType      Class of the configuration to return
     * @param <T>             type of the configuration to return
     * @return the object for this configuration.
     */
    public <T> Supplier<T> getObjectSupplier(ConfigurationKey key, T defaultValue, Class<T> objectType);
}
