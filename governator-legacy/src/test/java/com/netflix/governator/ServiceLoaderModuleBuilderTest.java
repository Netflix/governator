package com.netflix.governator;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.name.Names;
import com.google.inject.util.Types;
import com.netflix.governator.guice.serviceloader.MyServiceLoadedModule;
import com.netflix.governator.guice.serviceloader.ServiceLoaderTest.BoundServiceImpl;
import com.netflix.governator.guice.serviceloader.TestService;

import org.junit.Assert;
import org.junit.Test;

import java.util.Set;

public class ServiceLoaderModuleBuilderTest {
    @Test
    public void testMultibindings() {
        Injector injector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Boolean.class).toInstance(true);
                Multibinder
                    .newSetBinder(binder(), TestService.class)
                    .addBinding()
                    .to(BoundServiceImpl.class);
                
                install(new ServiceLoaderModuleBuilder().loadServices(TestService.class));
            }
        });
        
        Set<TestService> services = (Set<TestService>) injector.getInstance(Key.get(Types.setOf(TestService.class)));
        Assert.assertEquals(2, services.size());
        for (TestService service : services) {
            Assert.assertTrue(service.isInjected());
            System.out.println("   " + service.getClass().getName());
        }
    }
    
    @Test
    public void testServiceLoadedModules() {
        Injector injector = Guice.createInjector(
                new ServiceLoaderModuleBuilder().loadModules(Module.class));
        
        Assert.assertEquals("loaded",  injector.getInstance(Key.get(String.class, Names.named(MyServiceLoadedModule.class.getSimpleName()))));
    }

}
