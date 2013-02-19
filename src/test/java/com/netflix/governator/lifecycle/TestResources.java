package com.netflix.governator.lifecycle;

import com.google.inject.Injector;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.lifecycle.mocks.ObjectWithResources;
import org.testng.Assert;
import org.testng.annotations.Test;
import javax.annotation.Resource;
import javax.naming.NamingException;
import java.awt.*;
import java.math.BigInteger;
import java.util.concurrent.atomic.AtomicInteger;

public class TestResources
{
    @Test
    public void basicTest() throws Exception
    {
        final AtomicInteger classResourceCount = new AtomicInteger(0);
        final ResourceLocator resourceLocator = new ResourceLocator()
        {
            @Override
            public Object locate(Resource resource, ResourceLocator nextInChain) throws Exception
            {
                if ( resource.name().equals("com.netflix.governator.lifecycle.mocks.ObjectWithResources/myResource") )
                {
                    return "a";
                }

                if ( resource.name().equals("overrideInt") )
                {
                    return BigInteger.valueOf(2);
                }

                if ( resource.name().equals("com.netflix.governator.lifecycle.mocks.ObjectWithResources/p") )
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
        Injector injector = LifecycleInjector
            .builder()
            .withBootstrapModule
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

    @Test
    public void testChained() throws Exception
    {
        final AtomicInteger chainCount = new AtomicInteger(0);

        final ResourceLocator resourceLocator1 = new ResourceLocator()
        {
            @Override
            public Object locate(Resource resource, ResourceLocator nextInChain) throws Exception
            {
                chainCount.incrementAndGet();
                return nextInChain.locate(resource, nextInChain);
            }
        };
        final ResourceLocator resourceLocator2 = new ResourceLocator()
        {
            @Override
            public Object locate(Resource resource, ResourceLocator nextInChain) throws Exception
            {
                chainCount.incrementAndGet();
                return nextInChain.locate(resource, nextInChain);
            }
        };
        Injector injector = LifecycleInjector
            .builder()
            .withBootstrapModule
                (
                    new BootstrapModule()
                    {
                        @Override
                        public void configure(BootstrapBinder binder)
                        {
                            binder.bindResourceLocator().toInstance(resourceLocator1);
                            binder.bindResourceLocator().toInstance(resourceLocator2);
                        }
                    }
                )
            .createInjector();

        try
        {
            injector.getInstance(ObjectWithResources.class);
            Assert.fail("Should have thrown");
        }
        catch ( Throwable e )
        {
            while ( (e.getCause() != null) && !(e instanceof NamingException) )
            {
                e = e.getCause();
            }
            Assert.assertTrue(e instanceof NamingException);
        }

        Assert.assertEquals(chainCount.get(), 2);
    }
}
