package com.netflix.governator.lifecycle.warmup;

import com.google.common.collect.Sets;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TestWarmUpManager
{
    @Test
    public void     testDag1() throws Exception
    {
        Injector    injector = LifecycleInjector.builder().createInjector();
        injector.getInstance(Dag1.A.class);
        injector.getInstance(LifecycleManager.class).start();
        Recorder    recorder = injector.getInstance(Recorder.class);

        System.out.println(recorder.getRecordings());
        assertSingleExecution(recorder);
        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        assertOrdering(recorder, "A", "B");
        assertOrdering(recorder, "A", "C");
    }

    @Test
    public void     testDag2() throws Exception
    {
        Injector    injector = LifecycleInjector.builder().createInjector();
        injector.getInstance(Dag2.A1.class);
        injector.getInstance(Dag2.A2.class);
        injector.getInstance(Dag2.A3.class);
        injector.getInstance(LifecycleManager.class).start();
        Recorder    recorder = injector.getInstance(Recorder.class);

        System.out.println(recorder.getRecordings());
        assertSingleExecution(recorder);
        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        assertOrdering(recorder, "A1", "B1");
        assertOrdering(recorder, "B1", "C1");
        assertOrdering(recorder, "A1", "B2");
        assertOrdering(recorder, "B2", "C1");
        assertOrdering(recorder, "A2", "B2");
        assertOrdering(recorder, "B2", "C2");
        assertOrdering(recorder, "A2", "B3");
        assertOrdering(recorder, "B3", "C2");
        assertOrdering(recorder, "A3", "B3");
        assertOrdering(recorder, "B3", "C3");
        assertOrdering(recorder, "A3", "B4");
        assertOrdering(recorder, "B4", "C3");
    }

    @Test
    public void     testDag3() throws Exception
    {
        Injector    injector = LifecycleInjector.builder().createInjector();
        injector.getInstance(Dag3.A.class);
        injector.getInstance(LifecycleManager.class).start();
        Recorder    recorder = injector.getInstance(Recorder.class);

        System.out.println(recorder.getRecordings());
        assertSingleExecution(recorder);
        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        assertOrdering(recorder, "A", "C");
        assertOrdering(recorder, "C", "D");
        assertOrdering(recorder, "A", "D");
        assertOrdering(recorder, "B", "D");
    }

    @Test
    public void     testDag4() throws Exception
    {
        Injector    injector = LifecycleInjector
            .builder()
            .withModules
            (
                new Module()
                {
                    @Override
                    public void configure(Binder binder)
                    {
                        RecorderSleepSettings recorderSleepSettings = new RecorderSleepSettings();
                        recorderSleepSettings.setBaseSleep(1, TimeUnit.SECONDS);
                        recorderSleepSettings.setBaseSleepFor("E", 1, TimeUnit.MILLISECONDS);
                        recorderSleepSettings.setRandomize(false);
                        binder.bind(RecorderSleepSettings.class).toInstance(recorderSleepSettings);
                    }
                }
            )
            .createInjector();
        injector.getInstance(Dag4.A.class);
        injector.getInstance(LifecycleManager.class).start();
        Recorder    recorder = injector.getInstance(Recorder.class);

        System.out.println(recorder.getRecordings());
        assertSingleExecution(recorder);
        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        assertOrdering(recorder, "D", "E");
        assertOrdering(recorder, "D", "F");
        assertOrdering(recorder, "C", "E");
        assertOrdering(recorder, "B", "D");
        assertOrdering(recorder, "A", "B");
    }

    @Test
    public void     testStuck() throws Exception
    {
        Injector    injector = LifecycleInjector
            .builder()
            .withModules
            (
                new Module()
                {
                    @Override
                    public void configure(Binder binder)
                    {
                        RecorderSleepSettings recorderSleepSettings = new RecorderSleepSettings();
                        recorderSleepSettings.setBaseSleepFor("C", 1, TimeUnit.DAYS);
                        binder.bind(RecorderSleepSettings.class).toInstance(recorderSleepSettings);
                    }
                }
            )
            .createInjector();
        injector.getInstance(Dag1.A.class);
        boolean     succeeded = injector.getInstance(LifecycleManager.class).start(5, TimeUnit.SECONDS);
        Recorder    recorder = injector.getInstance(Recorder.class);

        System.out.println(recorder.getRecordings());
        assertSingleExecution(recorder);
        Assert.assertFalse(succeeded);
        Assert.assertTrue(recorder.getRecordings().contains("B"));
        Assert.assertEquals(recorder.getInterruptions(), Arrays.asList("C"));
    }

    private void        assertSingleExecution(Recorder recorder)
    {
        Set<String>     duplicateCheck = Sets.newHashSet();
        for ( String s : recorder.getRecordings() )
        {
            Assert.assertFalse(duplicateCheck.contains(s), s + " ran more than once: " + recorder.getRecordings());
            duplicateCheck.add(s);
        }
    }

    private void        assertOrdering(Recorder recorder, String base, String dependency)
    {
        int     baseIndex = recorder.getRecordings().indexOf(base);
        int     dependencyIndex = recorder.getRecordings().indexOf(dependency);

        Assert.assertTrue(baseIndex >= 0);
        Assert.assertTrue(dependencyIndex >= 0);
        Assert.assertTrue(baseIndex > dependencyIndex, "baseIndex: " + baseIndex + " - dependencyIndex: " + dependencyIndex);
    }
}
