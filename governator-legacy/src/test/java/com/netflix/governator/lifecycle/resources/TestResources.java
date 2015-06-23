package com.netflix.governator.lifecycle.resources;

import com.google.inject.Injector;
import com.netflix.governator.LifecycleInjectorBuilderProvider;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.lifecycle.ResourceLocator;
import org.testng.Assert;
import org.testng.annotations.Test;
import javax.annotation.Resource;
import java.awt.*;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

public class TestResources extends LifecycleInjectorBuilderProvider
{
    @Test(dataProvider = "builders")
    public void basicTest(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        final AtomicInteger classResourceCount = new AtomicInteger(0);
        final ResourceLocator resourceLocator = new ResourceLocator()
        {
            @Override
            public Object locate(Resource resource, ResourceLocator nextInChain) throws Exception
            {
                if ( resource.name().equals(ObjectWithResources.class.getName() + "/myResource") )
                {
                    return "a";
                }

                if ( resource.name().equals("overrideInt") )
                {
                    return BigInteger.valueOf(2);
                }

                if ( resource.name().equals(ObjectWithResources.class.getName() + "/p") )
                {
                    return new Point(3, 4);
                }

                if ( resource.name().equals("overrideRect") )
                {
                    return new Rectangle(5, 6);
                }

                if ( resource.name().equals("classResource") )
                {
                    classResourceCount.incrementAndGet();
                    return 7.8;
                }

                return null;
            }
        };
        Injector injector = lifecycleInjectorBuilder.withBootstrapModule
            (
                new BootstrapModule()
                {
                    @Override
                    public void configure(BootstrapBinder binder)
                    {
                        binder.bindResourceLocator().toInstance(resourceLocator);
                    }
                }
            )
            .createInjector();

        ObjectWithResources obj = injector.getInstance(ObjectWithResources.class);
        Assert.assertEquals(obj.getMyResource(), "a");
        Assert.assertEquals(obj.getMyOverrideResource(), BigInteger.valueOf(2));
        Assert.assertEquals(obj.getP(), new Point(3, 4));
        Assert.assertEquals(obj.getR(), new Rectangle(5, 6));
        Assert.assertEquals(classResourceCount.get(), 1);
    }

    @Test(dataProvider = "builders")
    public void testChained(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        final AtomicInteger resourceLocator1Count = new AtomicInteger(0);
        final AtomicInteger resourceLocator2Count = new AtomicInteger(0);

        final ResourceLocator resourceLocator1 = new ResourceLocator()
        {
            @Override
            public Object locate(Resource resource, ResourceLocator nextInChain) throws Exception
            {
                resourceLocator1Count.incrementAndGet();
                return nextInChain.locate(resource, nextInChain);
            }
        };
        final ResourceLocator resourceLocator2 = new ResourceLocator()
        {
            @Override
            public Object locate(Resource resource, ResourceLocator nextInChain) throws Exception
            {
                resourceLocator2Count.incrementAndGet();
                return nextInChain.locate(resource, nextInChain);
            }
        };
        Injector injector = lifecycleInjectorBuilder.withBootstrapModule
            (
                new BootstrapModule()
                {
                    @Override
                    public void configure(BootstrapBinder binder)
                    {
                        binder.bindResourceLocator().toInstance(resourceLocator1);
                        binder.bindResourceLocator().toInstance(resourceLocator2);
                        binder.bind(BigInteger.class).toInstance(BigInteger.valueOf(1));
                        binder.bind(Double.class).toInstance(1.1);
                    }
                }
            )
            .createInjector();

        injector.getInstance(ObjectWithResources.class);
        Assert.assertEquals(resourceLocator1Count.get(), 5);    // 1 for each @Resource
        Assert.assertEquals(resourceLocator2Count.get(), 5);    //      "       "
    }
}
