package com.netflix.governator.lifecycle.resources;

import com.google.inject.Injector;
import com.google.inject.Scopes;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * @author Alexey Krylov (lexx)
 * @since 19.02.13
 */
public class Jsr250SupportTest
{
    @Test
    public void testJsr250EnabledService() throws Exception
    {
        Injector injector = LifecycleInjector.builder().createInjector();
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
