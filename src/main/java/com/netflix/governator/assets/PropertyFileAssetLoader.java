package com.netflix.governator.assets;

import com.google.common.io.Closeables;
import com.google.common.io.Resources;
import com.netflix.governator.configuration.PropertiesConfigurationProvider;
import com.netflix.governator.lifecycle.LifecycleConfigurationProviders;
import javax.inject.Inject;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * An asset loader that reads a properties file (the RequiredAsset value)
 * and adds a ConfigurationProvider with those properties to the configuration chain
 */
public class PropertyFileAssetLoader implements AssetLoader
{
    private final LifecycleConfigurationProviders configurationProvider;

    @Inject
    public PropertyFileAssetLoader(LifecycleConfigurationProviders configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }

    @Override
    public void loadAsset(String name) throws Exception
    {
        URL             url = Resources.getResource(name);

        Properties      properties = new Properties();
        InputStream     in = null;
        try
        {
            in = url.openStream();
            properties.load(in);
        }
        finally
        {
            Closeables.closeQuietly(in);
        }

        configurationProvider.add(new PropertiesConfigurationProvider(properties));
    }

    @Override
    public void unloadAsset(String name) throws Exception
    {
    }
}
