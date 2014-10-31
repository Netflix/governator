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

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;

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
    public Property<Boolean> getBooleanProperty(ConfigurationKey key, Boolean defaultValue)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getBooleanProperty(key, defaultValue);
            }
        }
        return null;
    }

    @Override
    public Property<Integer> getIntegerProperty(ConfigurationKey key, Integer defaultValue)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getIntegerProperty(key, defaultValue);
            }
        }
        return null;
    }

    @Override
    public Property<Long> getLongProperty(ConfigurationKey key, Long defaultValue)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getLongProperty(key, defaultValue);
            }
        }
        return null;
    }

    @Override
    public Property<Double> getDoubleProperty(ConfigurationKey key, Double defaultValue)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getDoubleProperty(key, defaultValue);
            }
        }
        return null;
    }

    @Override
    public Property<String> getStringProperty(ConfigurationKey key, String defaultValue)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getStringProperty(key, defaultValue);
            }
        }
        return null;
    }

    @Override
    public Property<Date> getDateProperty(ConfigurationKey key, Date defaultValue)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getDateProperty(key, defaultValue);
            }
        }
        return null;
    }

    @Override
    public <T> Property<T> getObjectProperty(ConfigurationKey key, T defaultValue, Class<T> objectType)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getObjectProperty(key, defaultValue, objectType);
            }
        }
        return null;
    }
}
