package com.netflix.governator.lifecycle.mocks;

import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.PreConfiguration;
import com.netflix.governator.configuration.CompositeConfigurationProvider;
import com.netflix.governator.configuration.PropertiesConfigurationProvider;
import org.testng.Assert;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Properties;

public class PreConfigurationChange
{
    private final CompositeConfigurationProvider configurationProvider;

    @Configuration("pre-config-test")
    private String      value = "default";

    @Inject
    public PreConfigurationChange(CompositeConfigurationProvider configurationProvider)
    {
        this.configurationProvider = configurationProvider;
    }

    @PreConfiguration
    public void     preConfiguration()
    {
        Assert.assertEquals(value, "default");

        Properties      override = new Properties();
        override.setProperty("pre-config-test", "override");
        configurationProvider.add(new PropertiesConfigurationProvider(override));
    }

    @PostConstruct
    public void     postConstruct()
    {
        Assert.assertEquals(value, "override");
    }
}
