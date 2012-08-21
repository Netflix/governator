package com.netflix.governator.guice;

import com.google.common.collect.Maps;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.netflix.governator.guice.mocks.MockWithRequiredAsset;
import com.netflix.governator.guice.mocks.ParameterizedAssetLoader;
import org.testng.annotations.Test;
import java.util.Map;

public class TestAssetParameters
{
    @Test
    public void     testAssetParameters()
    {
        Injector        injector = LifecycleInjector.builder()
            .withBootstrapModule
            (
                new BootstrapModule()
                {
                    @Override
                    public void configure(BootstrapBinder binder)
                    {
                        binder.bindAssetLoader("test").to(ParameterizedAssetLoader.class);

                        Map<String, Integer>    intMap = Maps.newHashMap();
                        intMap.put("one", 1);
                        intMap.put("two", 2);
                        intMap.put("three", 3);
                        binder.bindAssetParameter("test", String.class).toInstance("Yes it's a test");
                        binder.bindAssetParameter("test", new TypeLiteral<Map<String, Integer>>(){}).toInstance(intMap);
                    }
                }
            )
            .createInjector();

        injector.getInstance(MockWithRequiredAsset.class);
        // assertions are in ParameterizedAssetLoader
    }
}
