package com.netflix.governator.lifecycle.warmup;

import com.google.inject.Injector;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import java.util.Arrays;

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

        Assert.assertEquals(Dag1.recorder.get().size(), 3);
        Assert.assertEquals(Dag1.recorder.get().get(2), "A");
    }

    @Test
    public void     testDag2() throws Exception
    {
        Injector    injector = LifecycleInjector.builder().createInjector();
        injector.getInstance(Dag2.A1.class);
        injector.getInstance(Dag2.A2.class);
        injector.getInstance(Dag2.A3.class);
        injector.getInstance(LifecycleManager.class).start();

        Assert.assertEquals(Dag2.recorder.get().size(), 10);
        for ( int i = 0; i < 3; ++i )
        {
            Assert.assertTrue(Dag2.recorder.get().get(i).startsWith("C"));
        }
        for ( int i = 3; i < 7; ++i )
        {
            Assert.assertTrue(Dag2.recorder.get().get(i).startsWith("B"));
        }
        for ( int i = 7; i < 10; ++i )
        {
            Assert.assertTrue(Dag2.recorder.get().get(i).startsWith("A"));
        }
    }

    @Test
    public void     testDag3() throws Exception
    {
        Injector    injector = LifecycleInjector.builder().createInjector();
        injector.getInstance(Dag3.A.class);
        injector.getInstance(LifecycleManager.class).start();

        Assert.assertEquals(Dag3.recorder.get().size(), 4);
        Assert.assertEquals(Dag3.recorder.get().get(0), "D");
        Assert.assertTrue(Dag3.recorder.get().get(1).equals("B") || Dag3.recorder.get().get(1).equals("C"), Dag3.recorder.get().get(1));
        Assert.assertTrue(Dag3.recorder.get().get(2).equals("B") || Dag3.recorder.get().get(2).equals("C"), Dag3.recorder.get().get(2));
        Assert.assertEquals(Dag3.recorder.get().get(3), "A");
    }
}
