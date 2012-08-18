package com.netflix.governator.guice;

import com.google.inject.Injector;
import com.netflix.governator.configuration.PropertiesConfigurationProvider;
import com.netflix.governator.guice.mocks.AssetLoadingMock;
import com.netflix.governator.guice.mocks.ObjectWithConfig;
import com.netflix.governator.lifecycle.LifecycleManager;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Properties;

public class TestInjectedConfiguration
{
    @Test
    public void     testPropertyLoadingViaAsset() throws Exception
    {
        Injector            injector = LifecycleInjector.builder().usingBasePackages("com.netflix.governator.guice.mocks").createInjector();

        AssetLoadingMock    mock = injector.getInstance(AssetLoadingMock.class);
        Assert.assertEquals(mock.anInt, 10);
        Assert.assertEquals(mock.aDouble, 20.3);
        Assert.assertEquals(mock.aString, "Who is John Galt?");
        Assert.assertEquals(mock.shouldBeDefault, "default");

        injector.getInstance(LifecycleManager.class).start();
    }

    @Test
    public void     testConfigurationProvider() throws Exception
    {
        final Properties    properties = new Properties();
        properties.setProperty("a", "1");
        properties.setProperty("b", "2");
        properties.setProperty("c", "3");

        Injector            injector = LifecycleInjector.builder()
            .withBootstrapModule
            (
                new BootstrapModule()
                {
                    @Override
                    public void configure(BootstrapBinder binder)
                    {
                        binder.bindConfigurationProvider().toInstance(new PropertiesConfigurationProvider(properties));
                    }
                }
            )
            .createInjector();

        ObjectWithConfig        obj = injector.getInstance(ObjectWithConfig.class);
        Assert.assertEquals(obj.a, 1);
        Assert.assertEquals(obj.b, 2);
        Assert.assertEquals(obj.c, 3);
    }
}
