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

import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.netflix.governator.LifecycleInjectorBuilderProvider;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.LifecycleManager;

public class TestWarmUpManager extends LifecycleInjectorBuilderProvider {
    private static final Logger LOG = LoggerFactory.getLogger(TestWarmUpManager.class);

    @Test
    public void testPostStart() throws Exception {
        Injector injector = LifecycleInjector.builder().build().createInjector();
        injector.getInstance(LifecycleManager.class).start();

        injector.getInstance(Dag1.A.class);
        Recorder recorder = injector.getInstance(Recorder.class);

        LOG.info("" + recorder.getRecordings());
        LOG.info("" + recorder.getConcurrents());

        assertSingleExecution(recorder);
        assertNotConcurrent(recorder, "A", "B");
        assertNotConcurrent(recorder, "A", "C");

        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        assertOrdering(recorder, "A", "B");
        assertOrdering(recorder, "A", "C");
    }

    @Test(expected = Error.class)
    public void testErrors() throws Exception {
        AbstractModule module = new AbstractModule() {
            @Override
            protected void configure() {
                binder().bind(WarmUpWithException.class).asEagerSingleton();
            }
        };
        LifecycleInjector.builder().withModules(module).build().createInjector().getInstance(LifecycleManager.class)
                .start();
    }

    @Test
    public void testDag1MultiModule() throws Exception {
        final List<AbstractModule> modules = Arrays.asList(new AbstractModule() {
            @Override
            protected void configure() {
                bind(Dag1.A.class);
            }
        }, new AbstractModule() {
            @Override
            protected void configure() {
                bind(Dag1.B.class);
            }
        }, new AbstractModule() {
            @Override
            protected void configure() {
                bind(Dag1.C.class);
            }
        });
        Injector injector = LifecycleInjector.builder().withModules(modules).build().createInjector();
        injector.getInstance(LifecycleManager.class).start();
        Recorder recorder = injector.getInstance(Recorder.class);

        LOG.info("" + recorder.getRecordings());
        LOG.info("" + recorder.getConcurrents());

        assertSingleExecution(recorder);
        assertNotConcurrent(recorder, "A", "B");
        assertNotConcurrent(recorder, "A", "C");

        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        assertOrdering(recorder, "A", "B");
        assertOrdering(recorder, "A", "C");
    }

    @Test
    public void testDagInterfaceModule() throws Exception {
        final Module dag1Module = new AbstractModule() {
            @Override
            protected void configure() {
                bind(DagInterface.A.class).to(DagInterface.AImpl.class);
                bind(DagInterface.B.class).to(DagInterface.BImpl.class);
                bind(DagInterface.C.class).to(DagInterface.CImpl.class);
            }
        };
        Injector injector = LifecycleInjector.builder().withModules(dag1Module).build().createInjector();
        injector.getInstance(LifecycleManager.class).start();
        Recorder recorder = injector.getInstance(Recorder.class);

        LOG.info("" + recorder.getRecordings());
        LOG.info("" + recorder.getConcurrents());

        assertSingleExecution(recorder);
        assertNotConcurrent(recorder, "A", "B");
        assertNotConcurrent(recorder, "A", "C");

        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        assertOrdering(recorder, "A", "B");
        assertOrdering(recorder, "A", "C");
    }

    @Test
    public void testDag1() throws Exception {
        Injector injector = LifecycleInjector.builder().build().createInjector();
        injector.getInstance(Dag1.A.class);
        injector.getInstance(LifecycleManager.class).start();
        Recorder recorder = injector.getInstance(Recorder.class);

        LOG.info("" + recorder.getRecordings());
        LOG.info("" + recorder.getConcurrents());

        assertSingleExecution(recorder);
        assertNotConcurrent(recorder, "A", "B");
        assertNotConcurrent(recorder, "A", "C");

        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        assertOrdering(recorder, "A", "B");
        assertOrdering(recorder, "A", "C");
    }

    @Test
    public void testDag2() throws Exception {
        Injector injector = LifecycleInjector.builder().build().createInjector();
        injector.getInstance(Dag2.A1.class);
        injector.getInstance(Dag2.A2.class);
        injector.getInstance(Dag2.A3.class);
        injector.getInstance(LifecycleManager.class).start();
        Recorder recorder = injector.getInstance(Recorder.class);

        LOG.info("" + recorder.getRecordings());
        LOG.info("" + recorder.getConcurrents());

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

    @Test
    public void testDag3() throws Exception {
        Injector injector = LifecycleInjector.builder().build().createInjector();
        injector.getInstance(Dag3.A.class);
        injector.getInstance(LifecycleManager.class).start();
        Recorder recorder = injector.getInstance(Recorder.class);

        LOG.info("" + recorder.getRecordings());
        LOG.info("" + recorder.getConcurrents());

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

    @Test
    public void testDag4() throws Exception {
        Injector injector = LifecycleInjector.builder().build().createInjector();
        injector.getInstance(Dag4.A.class);
        injector.getInstance(LifecycleManager.class).start();
        Recorder recorder = injector.getInstance(Recorder.class);

        LOG.info("" + recorder.getRecordings());
        LOG.info("" + recorder.getConcurrents());

        assertSingleExecution(recorder);
        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        assertOrdering(recorder, "D", "E");
        assertOrdering(recorder, "C", "E");
        assertOrdering(recorder, "B", "D");
        assertOrdering(recorder, "A", "B");
    }

    @Test
    public void testFlat() throws Exception {
        Injector injector = LifecycleInjector.builder().build().createInjector();
        Recorder recorder = injector.getInstance(Recorder.class);
        injector.getInstance(Flat.A.class).recorder = recorder;
        injector.getInstance(Flat.B.class).recorder = recorder;
        injector.getInstance(LifecycleManager.class).start();

        LOG.info("" + recorder.getRecordings());
        LOG.info("" + recorder.getConcurrents());

        assertSingleExecution(recorder);
        Assert.assertEquals(recorder.getInterruptions().size(), 0);
        Assert.assertTrue(recorder.getRecordings().indexOf("A") >= 0);
        Assert.assertTrue(recorder.getRecordings().indexOf("B") >= 0);
    }

    private void assertSingleExecution(Recorder recorder) {
        Set<String> duplicateCheck = Sets.newHashSet();
        for (String s : recorder.getRecordings()) {
            Assert.assertFalse(s + " ran more than once: " + recorder.getRecordings(), duplicateCheck.contains(s));
            duplicateCheck.add(s);
        }
    }

    private void assertOrdering(Recorder recorder, String base, String dependency) {
        int baseIndex = recorder.getRecordings().indexOf(base);
        int dependencyIndex = recorder.getRecordings().indexOf(dependency);

        Assert.assertTrue(baseIndex >= 0);
        Assert.assertTrue(dependencyIndex >= 0);
        Assert.assertTrue("baseIndex: " + baseIndex + " - dependencyIndex: " + dependencyIndex,
                baseIndex > dependencyIndex);
    }

    private void assertNotConcurrent(Recorder recorder, String task1, String task2) {
        for (Set<String> s : recorder.getConcurrents()) {
            Assert.assertTrue(String.format("Incorrect concurrency for %s and %s: %s", task1, task2, s),
                    !s.contains(task1) || !s.contains(task2));
        }
    }
}
