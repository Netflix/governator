package com.netflix.governator.auto;

import java.util.Properties;

import junit.framework.Assert;

import org.testng.annotations.Test;

import com.google.inject.Injector;
import com.google.inject.Stage;
import com.netflix.governator.Governator;

public class AutoModuleTest {
    @Test
    public void test() {
        final Properties prop = new Properties();
        prop.setProperty("test", "B");
                
        Injector injector = Governator
            .createInjector(Stage.DEVELOPMENT, 
                AutoModuleBuilder
                    .forModule(new AModule())
                    .withBootstrap(PropertiesConfig.toModule(prop))
                    .withProfile("test")
                    .withAutoLoadActiveProfiles(true)
                    .build());
        
        System.out.println(injector.getInstance(String.class));
        Assert.assertEquals("override", injector.getInstance(String.class));
        Assert.assertEquals("override", injector.getInstance(String.class));
    }
}
