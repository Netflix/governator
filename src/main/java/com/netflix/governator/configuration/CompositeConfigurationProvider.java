package com.netflix.governator.configuration;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A configuration provider that composites multiple providers. The first
 * provider (in order) that has a configuration set (via {@link #has(String)} is used
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
    public boolean has(String name)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(name) )
            {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean getBoolean(String name)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(name) )
            {
                return provider.getBoolean(name);
            }
        }
        return false;
    }

    @Override
    public int getInteger(String name)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(name) )
            {
                return provider.getInteger(name);
            }
        }
        return 0;
    }

    @Override
    public long getLong(String name)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(name) )
            {
                return provider.getLong(name);
            }
        }
        return 0;
    }

    @Override
    public double getDouble(String name)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(name) )
            {
                return provider.getDouble(name);
            }
        }
        return 0;
    }

    @Override
    public String getString(String name)
    {
        for ( ConfigurationProvider provider : providers )
        {
            if ( provider.has(name) )
            {
                return provider.getString(name);
            }
        }
        return null;
    }
}
