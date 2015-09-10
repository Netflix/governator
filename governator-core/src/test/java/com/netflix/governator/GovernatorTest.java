package com.netflix.governator;

import javax.inject.Singleton;

import junit.framework.Assert;

import org.testng.annotations.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.netflix.governator.auto.ModuleListProviders;
import com.netflix.governator.auto.annotations.ConditionalOnModule;
import com.netflix.governator.auto.annotations.OverrideModule;

public class GovernatorTest {
    public interface Foo {
    }
    
    @Singleton
    public static class Foo1 implements Foo {
    }
    
    @Singleton
    public static class Foo2 implements Foo {
    }
    
    @Singleton 
    public static class Foo3 implements Foo {
    }
    
    public interface Bar {
        
    }
    
    @Singleton
    public static class Bar1 implements Bar {
    }
    
    @Singleton
    public static class Bar2 implements Bar {
    }
    
    @Singleton 
    public static class Bar3 implements Bar {
    }
    
    public abstract static class BaseModule extends AbstractModule {
        @Override
        public String toString() {
            return getClass().getSimpleName();
        }
    }
    
    public static class Foo1Module extends BaseModule {
        @Override
        protected void configure() {
            bind(Foo.class).to(Foo1.class);
        }
    }
    
    @ConditionalOnModule(Foo1Module.class)
    @OverrideModule
    public static class Foo2Module extends BaseModule {
        @Override
        protected void configure() {
            bind(Foo.class).to(Foo2.class);
        }
    }
    
    public static class Foo3OverrideModule extends BaseModule {
        @Override
        protected void configure() {
            bind(Foo.class).to(Foo3.class);
        }
    }
    
    @Test
    public void autoLoadAsOverride() throws Exception {
        Injector injector = Governator.createInjector(
                DefaultGovernatorConfiguration.builder()
                    .addModule(new Foo1Module())
                    .addModuleListProvider(ModuleListProviders.forModules(new Foo2Module()))
                    .build());
        
        Foo foo = injector.getInstance(Foo.class);
        Assert.assertEquals(Foo2.class, foo.getClass());
    }
    
    @Test
    public void autoLoadWithOverride() throws Exception {
        Injector injector = Governator.createInjector(
                DefaultGovernatorConfiguration.builder()
                    .addModule(new Foo1Module())
                    .addModuleListProvider(ModuleListProviders.forModules(new Foo2Module()))
                    .addOverrideModule(new Foo3OverrideModule())
                    .build());
        
        Foo foo = injector.getInstance(Foo.class);
        Assert.assertEquals(Foo3.class, foo.getClass());
    }
}
