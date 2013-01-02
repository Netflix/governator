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

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
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
    public boolean getBoolean(ConfigurationKey key)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getBoolean(key);
            }
        }
        return false;
    }

    @Override
    public int getInteger(ConfigurationKey key)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getInteger(key);
            }
        }
        return 0;
    }

    @Override
    public long getLong(ConfigurationKey key)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getLong(key);
            }
        }
        return 0;
    }

    @Override
    public double getDouble(ConfigurationKey key)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getDouble(key);
            }
        }
        return 0;
    }

    @Override
    public String getString(ConfigurationKey key)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(key) )
            {
                return provider.getString(key);
            }
        }
        return null;
    }
}
