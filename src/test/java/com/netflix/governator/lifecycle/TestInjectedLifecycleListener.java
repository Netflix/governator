package com.netflix.governator.lifecycle;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Collection;

public class TestInjectedLifecycleListener
{
    public interface TestInterface
    {
        public String       getValue();
    }

    public static class MyListener implements LifecycleListener
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
        public void objectInjected(Object obj)
        {
        }

        @Override
        public void stateChanged(Object obj, LifecycleState newState)
        {
        }
    }

    @Test
    public void     testInjectedLifecycleListener() throws Exception
    {
        Injector injector = LifecycleInjector.builder()
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
