package com.netflix.governator.lifecycle.warmup;

import com.google.inject.Injector;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;
import org.testng.annotations.Test;

public class TestWarmUpManager
{
    @Test
    public void     testSimple() throws Exception
    {
        Injector    injector = LifecycleInjector.builder().createInjector();
        injector.getInstance(Dag1.A.class);
        injector.getInstance(LifecycleManager.class).start();
    }
}
