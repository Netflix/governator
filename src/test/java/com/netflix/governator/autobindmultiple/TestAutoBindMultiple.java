package com.netflix.governator.autobindmultiple;

import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import com.netflix.governator.autobindmultiple.basic.BaseForMocks;
import com.netflix.governator.autobindmultiple.generic.BaseForGenericMocks;
import com.netflix.governator.guice.LifecycleInjector;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Set;

public class TestAutoBindMultiple
{
    @Singleton
    public static class Container
    {
        private final Set<BaseForMocks> mocks;

        @Inject
        public Container(Set<BaseForMocks> mocks)
        {
            this.mocks = mocks;
        }

        public Set<BaseForMocks> getMocks()
        {
            return mocks;
        }
    }

    @Singleton
    public static class GenericContainer
    {
        private final Set<BaseForGenericMocks<Integer>> mocks;

        @Inject
        public GenericContainer(Set<BaseForGenericMocks<Integer>> mocks)
        {
            this.mocks = mocks;
        }

        public Set<BaseForGenericMocks<Integer>> getMocks()
        {
            return mocks;
        }
    }

    @Test
    public void testBasic()
    {
        Injector injector = LifecycleInjector.builder().usingBasePackages("com.netflix.governator.autobindmultiple.basic").createInjector();
        Container container = injector.getInstance(Container.class);
        Assert.assertEquals(container.getMocks().size(), 3);
        Iterable<String> transformed = Iterables.transform
        (
            container.getMocks(),
            new Function<BaseForMocks, String>()
            {
                @Override
                public String apply(BaseForMocks mock)
                {
                    return mock.getValue();
                }
            }
        );
        Assert.assertEquals(Sets.<String>newHashSet(transformed), Sets.newHashSet("A", "B", "C"));
    }

    @Test
    public void testGeneric()
    {
        Injector injector = LifecycleInjector.builder().usingBasePackages("com.netflix.governator.autobindmultiple.generic").createInjector();
        GenericContainer container = injector.getInstance(GenericContainer.class);
        Assert.assertEquals(container.getMocks().size(), 3);
        Iterable<Integer> transformed = Iterables.transform
        (
            container.getMocks(),
            new Function<BaseForGenericMocks<Integer>, Integer>()
            {
                @Override
                public Integer apply(BaseForGenericMocks<Integer> mock)
                {
                    return mock.getValue();
                }
            }
        );
        Assert.assertEquals(Sets.<Integer>newHashSet(transformed), Sets.newHashSet(1, 2, 3));
    }
}
