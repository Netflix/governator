package com.netflix.governator.lifecycle.resources;

import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.netflix.governator.LifecycleInjectorBuilderProvider;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Alexey Krylov (lexx)
 * @since 19.02.13
 */
@RunWith(DataProviderRunner.class)
public class Jsr250SupportTest extends LifecycleInjectorBuilderProvider
{
    @Test @UseDataProvider("builders")
    public void testJsr250EnabledService(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        Injector injector = lifecycleInjectorBuilder.createInjector();
        Jsr250EnabledService jsr250EnabledService = injector.getInstance(Jsr250EnabledService.class);

        injector.getInstance(LifecycleManager.class).start();

        Assert.assertTrue(Scopes.isSingleton(injector.getBinding(jsr250EnabledService.getClass())));
        Assert.assertTrue(jsr250EnabledService.isPostConstuctInvoked());

        Assert.assertTrue(jsr250EnabledService.isResourceSet());
        Jsr250EnabledService service = injector.getInstance(Jsr250EnabledService.class);
        Assert.assertEquals(jsr250EnabledService.getResource(), service.getResource());

        injector.getInstance(LifecycleManager.class).close();
        Assert.assertTrue(jsr250EnabledService.isPreDestroyInvoked());
    }
}
