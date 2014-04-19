/*
 * Copyright 2013 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.governator.lifecycle.warmup;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Binder;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.netflix.governator.LifecycleInjectorBuilderProvider;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.LifecycleManagerArguments;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class TestWarmUpManager extends LifecycleInjectorBuilderProvider
{
    @Test(dataProvider = "builders")
    public void testPostStart(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        Injector injector = lifecycleInjectorBuilder.createInjector();
        injector.getInstance(LifecycleManager.class).start();

        injector.getInstance(Dag1.A.class);
        Recorder recorder = injector.getInstance(Recorder.class);

        Thread.sleep(LifecycleManagerArguments.DEFAULT_WARM_UP_PADDING_MS + 1000);

        System.out.println(recorder.getRecordings());
        System.out.println(recorder.getConcurrents());

        assertSingleExecution(recorder);
        assertNotConcurrent(recorder, "A", "B");
        assertNotConcurrent(recorder, "A", "C");

        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        assertOrdering(recorder, "A", "B");
        assertOrdering(recorder, "A", "C");
    }

    @SuppressWarnings("ThrowableResultOfMethodCallIgnored")
    @Test(dataProvider = "builders")
    public void testErrors(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        AbstractModule module = new AbstractModule()
        {
            @Override
            protected void configure()
            {
                binder().bind(WarmUpWithException.class).asEagerSingleton();
            }
        };
        try
        {
            lifecycleInjectorBuilder.withModules(module).createInjector().getInstance(LifecycleManager.class).start();
            Assert.fail("Should have thrown WarmUpException");
        }
        catch ( WarmUpException e )
        {
            e.printStackTrace();

            List<WarmUpErrors.Error> errors = Lists.newArrayList(e.getErrors());
            Assert.assertEquals(errors.size(), 1);
            Assert.assertEquals(errors.get(0).getException().getClass(), NullPointerException.class);
        }
    }

    @Test(dataProvider = "builders")
    public void testDag1MultiModule(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        final List<AbstractModule> modules = Arrays.asList(
            new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    bind(Dag1.A.class);
                }
            },
            new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    bind(Dag1.B.class);
                }
            },
            new AbstractModule()
            {
                @Override
                protected void configure()
                {
                    bind(Dag1.C.class);
                }
            }
                                                          );
        Injector injector = lifecycleInjectorBuilder.withModules(modules).build().createInjector();
        injector.getInstance(LifecycleManager.class).start();
        Recorder recorder = injector.getInstance(Recorder.class);

        System.out.println(recorder.getRecordings());
        System.out.println(recorder.getConcurrents());

        assertSingleExecution(recorder);
        assertNotConcurrent(recorder, "A", "B");
        assertNotConcurrent(recorder, "A", "C");

        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        assertOrdering(recorder, "A", "B");
        assertOrdering(recorder, "A", "C");
    }

    @Test(dataProvider = "builders")
    public void testDagInterfaceModule(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        final Module dag1Module = new AbstractModule()
        {
            @Override
            protected void configure()
            {
                bind(DagInterface.A.class).to(DagInterface.AImpl.class);
                bind(DagInterface.B.class).to(DagInterface.BImpl.class);
                bind(DagInterface.C.class).to(DagInterface.CImpl.class);
            }
        };
        Injector injector = lifecycleInjectorBuilder.withModules(dag1Module).build().createInjector();
        injector.getInstance(LifecycleManager.class).start();
        Recorder recorder = injector.getInstance(Recorder.class);

        System.out.println(recorder.getRecordings());
        System.out.println(recorder.getConcurrents());

        assertSingleExecution(recorder);
        assertNotConcurrent(recorder, "A", "B");
        assertNotConcurrent(recorder, "A", "C");

        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        assertOrdering(recorder, "A", "B");
        assertOrdering(recorder, "A", "C");
    }

    @Test(dataProvider = "builders")
    public void testDag1(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        Injector injector = lifecycleInjectorBuilder.createInjector();
        injector.getInstance(Dag1.A.class);
        injector.getInstance(LifecycleManager.class).start();
        Recorder recorder = injector.getInstance(Recorder.class);

        System.out.println(recorder.getRecordings());
        System.out.println(recorder.getConcurrents());

        assertSingleExecution(recorder);
        assertNotConcurrent(recorder, "A", "B");
        assertNotConcurrent(recorder, "A", "C");

        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        assertOrdering(recorder, "A", "B");
        assertOrdering(recorder, "A", "C");
    }

    @Test(dataProvider = "builders")
    public void testDag2(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        Injector injector = lifecycleInjectorBuilder.createInjector();
        injector.getInstance(Dag2.A1.class);
        injector.getInstance(Dag2.A2.class);
        injector.getInstance(Dag2.A3.class);
        injector.getInstance(LifecycleManager.class).start();
        Recorder recorder = injector.getInstance(Recorder.class);

        System.out.println(recorder.getRecordings());
        System.out.println(recorder.getConcurrents());

        assertSingleExecution(recorder);

        assertNotConcurrent(recorder, "A1", "B1");
        assertNotConcurrent(recorder, "A1", "B2");
        assertNotConcurrent(recorder, "B1", "C1");
        assertNotConcurrent(recorder, "B2", "C1");
        assertNotConcurrent(recorder, "A2", "B2");
        assertNotConcurrent(recorder, "A2", "B3");
        assertNotConcurrent(recorder, "B2", "C2");
        assertNotConcurrent(recorder, "B3", "C2");
        assertNotConcurrent(recorder, "A3", "B3");
        assertNotConcurrent(recorder, "A3", "B4");
        assertNotConcurrent(recorder, "B3", "C3");
        assertNotConcurrent(recorder, "B4", "C3");

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

    @Test(dataProvider = "builders")
    public void testDag3(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        Injector injector = lifecycleInjectorBuilder.createInjector();
        injector.getInstance(Dag3.A.class);
        injector.getInstance(LifecycleManager.class).start();
        Recorder recorder = injector.getInstance(Recorder.class);

        System.out.println(recorder.getRecordings());
        System.out.println(recorder.getConcurrents());

        assertSingleExecution(recorder);

        assertNotConcurrent(recorder, "C", "D");
        assertNotConcurrent(recorder, "B", "D");
        assertNotConcurrent(recorder, "A", "B");
        assertNotConcurrent(recorder, "A", "C");

        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        assertOrdering(recorder, "A", "C");
        assertOrdering(recorder, "C", "D");
        assertOrdering(recorder, "A", "D");
        assertOrdering(recorder, "B", "D");
    }

    @Test(dataProvider = "builders")
    public void testDag4(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        Injector injector = lifecycleInjectorBuilder
            .withModules(new Module()
                {
                    @Override
                    public void configure(Binder binder)
                    {
                        RecorderSleepSettings recorderSleepSettings = new RecorderSleepSettings();
                        recorderSleepSettings.setBaseSleepFor("E", 1, TimeUnit.MILLISECONDS);
                        recorderSleepSettings.setRandomize(false);
                        binder.bind(RecorderSleepSettings.class).toInstance(recorderSleepSettings);
                    }
                }
                        )
            .createInjector();
        injector.getInstance(Dag4.A.class);
        injector.getInstance(LifecycleManager.class).start();
        Recorder recorder = injector.getInstance(Recorder.class);

        System.out.println(recorder.getRecordings());
        System.out.println(recorder.getConcurrents());

        assertSingleExecution(recorder);
        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        assertOrdering(recorder, "D", "E");
        assertOrdering(recorder, "C", "E");
        assertOrdering(recorder, "B", "D");
        assertOrdering(recorder, "A", "B");
    }

    @Test(dataProvider = "builders")
    public void testFlat(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        Injector injector = lifecycleInjectorBuilder.createInjector();
        Recorder recorder = injector.getInstance(Recorder.class);
        injector.getInstance(Flat.A.class).recorder = recorder;
        injector.getInstance(Flat.B.class).recorder = recorder;
        injector.getInstance(LifecycleManager.class).start();

        System.out.println(recorder.getRecordings());
        System.out.println(recorder.getConcurrents());

        assertSingleExecution(recorder);
        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        Assert.assertTrue(recorder.getRecordings().indexOf("A") >= 0);
        Assert.assertTrue(recorder.getRecordings().indexOf("B") >= 0);
    }

    @Test(dataProvider = "builders")
    public void testStuck(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        Injector injector = lifecycleInjectorBuilder
            .withModules(new Module()
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
        boolean succeeded = injector.getInstance(LifecycleManager.class).start(5, TimeUnit.SECONDS);
        Recorder recorder = injector.getInstance(Recorder.class);

        System.out.println(recorder.getRecordings());
        System.out.println(recorder.getConcurrents());

        assertSingleExecution(recorder);
        Assert.assertFalse(succeeded);
        Assert.assertTrue(recorder.getRecordings().contains("B"));
        Assert.assertEquals(recorder.getInterruptions(), Arrays.asList("C"));
    }

    private void assertSingleExecution(Recorder recorder)
    {
        Set<String> duplicateCheck = Sets.newHashSet();
        for ( String s : recorder.getRecordings() )
        {
            Assert.assertFalse(duplicateCheck.contains(s), s + " ran more than once: " + recorder.getRecordings());
            duplicateCheck.add(s);
        }
    }

    private void assertOrdering(Recorder recorder, String base, String dependency)
    {
        int baseIndex = recorder.getRecordings().indexOf(base);
        int dependencyIndex = recorder.getRecordings().indexOf(dependency);

        Assert.assertTrue(baseIndex >= 0);
        Assert.assertTrue(dependencyIndex >= 0);
        Assert.assertTrue(baseIndex > dependencyIndex, "baseIndex: " + baseIndex + " - dependencyIndex: " + dependencyIndex);
    }

    private void assertNotConcurrent(Recorder recorder, String task1, String task2)
    {
        for ( Set<String> s : recorder.getConcurrents() )
        {
            Assert.assertTrue(!s.contains(task1) || !s.contains(task2), String.format("Incorrect concurrency for %s and %s: %s", task1, task2, s));
        }
    }
}
