package com.netflix.governator.lifecycle.warmup;

import com.google.inject.Injector;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class TestWarmUpManager
{
    @BeforeMethod
    public void     setup()
    {
        Dag1.recorder.clear();
        Dag2.recorder.clear();
        Dag3.recorder.clear();
    }

    @Test
    public void     testDag1() throws Exception
    {
        Injector    injector = LifecycleInjector.builder().createInjector();
        injector.getInstance(Dag1.A.class);
        injector.getInstance(LifecycleManager.class).start();

        assertOrdering(Dag1.recorder, "A", "B");
        assertOrdering(Dag1.recorder, "A", "C");
    }

    @Test
    public void     testDag2() throws Exception
    {
        Injector    injector = LifecycleInjector.builder().createInjector();
        injector.getInstance(Dag2.A1.class);
        injector.getInstance(Dag2.A2.class);
        injector.getInstance(Dag2.A3.class);
        injector.getInstance(LifecycleManager.class).start();

        assertOrdering(Dag2.recorder, "A1", "B1");
        assertOrdering(Dag2.recorder, "B1", "C1");
        assertOrdering(Dag2.recorder, "A1", "B2");
        assertOrdering(Dag2.recorder, "B2", "C1");
        assertOrdering(Dag2.recorder, "A2", "B2");
        assertOrdering(Dag2.recorder, "B2", "C2");
        assertOrdering(Dag2.recorder, "A2", "B3");
        assertOrdering(Dag2.recorder, "B3", "C2");
        assertOrdering(Dag2.recorder, "A3", "B3");
        assertOrdering(Dag2.recorder, "B3", "C3");
        assertOrdering(Dag2.recorder, "A3", "B4");
        assertOrdering(Dag2.recorder, "B4", "C3");
    }

    @Test
    public void     testDag3() throws Exception
    {
        Injector    injector = LifecycleInjector.builder().createInjector();
        injector.getInstance(Dag3.A.class);
        injector.getInstance(LifecycleManager.class).start();

        assertOrdering(Dag3.recorder, "A", "C");
        assertOrdering(Dag3.recorder, "C", "D");
        assertOrdering(Dag3.recorder, "A", "D");
        assertOrdering(Dag3.recorder, "B", "D");
    }

    private void        assertOrdering(Recorder recorder, String base, String dependency)
    {
        int     baseIndex = recorder.get().indexOf(base);
        int     dependencyIndex = recorder.get().indexOf(dependency);

        Assert.assertTrue(baseIndex >= 0);
        Assert.assertTrue(dependencyIndex >= 0);
        Assert.assertTrue(baseIndex > dependencyIndex, "baseIndex: " + baseIndex + " - dependencyIndex: " + dependencyIndex);
    }
}
