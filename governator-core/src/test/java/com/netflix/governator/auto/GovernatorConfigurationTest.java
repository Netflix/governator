package com.netflix.governator.auto;

import java.util.Properties;

import javax.inject.Singleton;

import junit.framework.Assert;

import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provider;
import com.google.inject.Provides;
import com.google.inject.spi.Elements;
import com.netflix.governator.DefaultGovernatorConfiguration;
import com.netflix.governator.ElementsEx;
import com.netflix.governator.Governator;
import com.netflix.governator.auto.modules.AModule;

public class GovernatorConfigurationTest {
    @Test
    public void test() {
        final Properties prop = new Properties();
        prop.setProperty("test", "B");
                
        Injector injector = Governator.createInjector(
                DefaultGovernatorConfiguration.builder()
                    .addProfile("test")
                    .addBootstrapModule(PropertiesPropertySource.toModule(prop))
                    .addModuleListProvider(ModuleListProviders.forPackagesConditional("com.netflix.governator.auto.modules"))
                    .build(),
                new AModule());
        
        System.out.println(injector.getInstance(String.class));
        Assert.assertEquals("override", injector.getInstance(String.class));
        Assert.assertEquals("override", injector.getInstance(String.class));
    }
    
    @Singleton
    public static class Foo {
    }
    
    @Singleton
    public static class Bar {
    }
    
    @Test
    public void testCopyBindings() {
        final Module m = new AbstractModule() {
            @Override
            protected void configure() {
            }
            
            @Provides
            @Singleton
            public Foo getFoo() {
                System.out.println("got foo");
                return new Foo();
            }
            
            @Provides
            @Singleton
            public Bar getBar() {
                System.out.println("got bar");
                return new Bar();
            }
        };
        
        final Injector injector = Guice.createInjector(m);
        Foo foo = injector.getInstance(Foo.class);
        
        Injector injector2 = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                for (Key<?> key : ElementsEx.getAllBoundKeys(Elements.getElements(m))) {
                    System.out.println("Copy binding : " + key);
                    Provider provider = injector.getBinding(key).getProvider();
                    bind(key).toProvider(provider);
                }
            }
        });
        
        Foo foo2 = injector2.getInstance(Foo.class);
        Assert.assertSame(foo, foo2);
        
        Bar bar = injector2.getInstance(Bar.class);
    }
}
