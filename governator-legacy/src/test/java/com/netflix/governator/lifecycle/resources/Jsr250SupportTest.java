package com.netflix.governator.lifecycle.resources;

import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.netflix.governator.LifecycleInjectorBuilderProvider;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.lifecycle.LifecycleManager;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Alexey Krylov (lexx)
 * @since 19.02.13
 */
public class Jsr250SupportTest extends LifecycleInjectorBuilderProvider
{
    @Test(dataProvider = "builders")
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
