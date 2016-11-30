package com.netflix.governator;

import java.util.Properties;

import javax.inject.Singleton;

import org.junit.Assert;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.configuration.ConfigurationProvider;
import com.netflix.governator.configuration.PropertiesConfigurationProvider;

public class ConfigurationModuleTest {
    public static class A {
        @Configuration("foo")
        public String foo;
    }
    
    @Test
    public void validateConfigurationMappingWorks() {
        try (LifecycleInjector injector = InjectorBuilder.fromModules(new AbstractModule() {
            @Override
            protected void configure() {
                install(new ConfigurationModule());
            }
            
            @Singleton
            @Provides
            ConfigurationProvider getConfigurationProvider() {
                Properties props = new Properties();
                props.setProperty("foo", "bar");
                
                return new PropertiesConfigurationProvider(props);
            }
        }).createInjector()) {
            A a = injector.getInstance(A.class);
            Assert.assertEquals("bar", a.foo);
        }
    }
}
