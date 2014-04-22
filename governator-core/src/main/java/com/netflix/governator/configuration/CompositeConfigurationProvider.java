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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Supplier;
import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A configuration provider that composites multiple providers. The first
 * provider (in order) that has a configuration set (via {@link #has(ConfigurationKey)} is used
 * to return the configuration.
 */
public class CompositeConfigurationProvider implements ConfigurationProvider
{
    private final List<ConfigurationProvider> providers;

    /**
     * @param providers ordered providers
     */
    public CompositeConfigurationProvider(ConfigurationProvider... providers)
    {
        this(Lists.newArrayList(Arrays.asList(providers)));
    }

    /**
     * @param providers ordered providers
     */
    public CompositeConfigurationProvider(Collection<ConfigurationProvider> providers)
    {
        this.providers = new CopyOnWriteArrayList<ConfigurationProvider>(providers);
    }

    @VisibleForTesting
    public void add(ConfigurationProvider configurationProvider)
    {
        providers.add(0, configurationProvider);
    }

    @Override
    public boolean has(ConfigurationKey key)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public Supplier<Boolean> getBooleanSupplier(ConfigurationKey key, Boolean defaultValue)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getBooleanSupplier(key, defaultValue);
            }
        }
        return null;
    }

    @Override
    public Supplier<Integer> getIntegerSupplier(ConfigurationKey key, Integer defaultValue)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getIntegerSupplier(key, defaultValue);
            }
        }
        return null;
    }

    @Override
    public Supplier<Long> getLongSupplier(ConfigurationKey key, Long defaultValue)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getLongSupplier(key, defaultValue);
            }
        }
        return null;
    }

    @Override
    public Supplier<Double> getDoubleSupplier(ConfigurationKey key, Double defaultValue)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getDoubleSupplier(key, defaultValue);
            }
        }
        return null;
    }

    @Override
    public Supplier<String> getStringSupplier(ConfigurationKey key, String defaultValue)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getStringSupplier(key, defaultValue);
            }
        }
        return null;
    }

    @Override
    public Supplier<Date> getDateSupplier(ConfigurationKey key, Date defaultValue)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getDateSupplier(key, defaultValue);
            }
        }
        return null;
    }

    @Override
    public <T> Supplier<T> getObjectSupplier(ConfigurationKey key, T defaultValue, Class<T> objectType)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getObjectSupplier(key, defaultValue, objectType);
            }
        }
        return null;
    }
}
