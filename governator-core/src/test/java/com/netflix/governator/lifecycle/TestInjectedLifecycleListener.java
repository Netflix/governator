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

package com.netflix.governator.lifecycle;

import java.util.Collection;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.TypeLiteral;
import com.netflix.governator.LifecycleInjectorBuilderProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjectorBuilder;

public class TestInjectedLifecycleListener extends LifecycleInjectorBuilderProvider
{
    public interface TestInterface
    {
        public String       getValue();
    }

    public static class MyListener extends DefaultLifecycleListener
    {
        private final TestInterface testInterface;

        @Inject
        public MyListener(TestInterface testInterface)
        {
            this.testInterface = testInterface;
        }

        public TestInterface getTestInterface()
        {
            return testInterface;
        }

        @Override
        public <T> void objectInjected(TypeLiteral<T> type, T obj)
        {
        }

        @Override
        public void stateChanged(Object obj, LifecycleState newState)
        {
        }
    }

    @Test(dataProvider = "builders")
    public void     testInjectedLifecycleListener(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        Injector injector = lifecycleInjectorBuilder
            .withBootstrapModule
            (
                new BootstrapModule()
                {
                    @Override
                    public void configure(BootstrapBinder binder)
                    {
                        TestInterface instance = new TestInterface()
                        {
                            @Override
                            public String getValue()
                            {
                                return "a is a";
                            }
                        };
                        binder.bind(TestInterface.class).toInstance(instance);
                        binder.bindLifecycleListener().to(MyListener.class);
                    }
                }
            )
            .createInjector();

        LifecycleManager                manager = injector.getInstance(LifecycleManager.class);
        Collection<LifecycleListener>   listeners = manager.getListeners();
        Assert.assertEquals(listeners.size(), 1);
        Assert.assertTrue(listeners.iterator().next() instanceof MyListener);
        Assert.assertEquals(((MyListener)listeners.iterator().next()).getTestInterface().getValue(), "a is a");
    }
}
