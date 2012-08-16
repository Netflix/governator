package com.netflix.governator.lifecycle;

import com.netflix.governator.lifecycle.mocks.ObjectWithParallelWarmUp;
import com.netflix.governator.lifecycle.mocks.ObjectWithWarmUp;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class TestWarmUp
{
    @Test
    public void     testSimple() throws Exception
    {
        ObjectWithWarmUp        obj = new ObjectWithWarmUp();
        LifecycleManager        manager = new LifecycleManager();
        manager.add(obj);
        manager.start();

        Assert.assertEquals(obj.warmUpCount.get(), 1);
        Assert.assertEquals(obj.coolDownCount.get(), 0);

        manager.close();
        Assert.assertEquals(obj.warmUpCount.get(), 1);
        Assert.assertEquals(obj.coolDownCount.get(), 1);
    }

    @Test
    public void     testParallel() throws Exception
    {
        final int               OBJECT_QTY = 10;

        final CountDownLatch    warmLatch = new CountDownLatch(OBJECT_QTY);
        final CountDownLatch    warmContinueLatch = new CountDownLatch(1);
        final CountDownLatch    coolLatch = new CountDownLatch(OBJECT_QTY);
        final CountDownLatch    coolContinueLatch = new CountDownLatch(1);
        LifecycleManager        manager = new LifecycleManager()
        {
            protected int getWarmUpThreadQty()
            {
                return OBJECT_QTY;
            }
        };
        for ( int i = 0; i < OBJECT_QTY; ++i )
        {
            Object      obj = new ObjectWithParallelWarmUp()
            {
                @Override
                public void warm()
                {
                    super.warm();
                    warmLatch.countDown();
                    try
                    {
                        warmContinueLatch.await();
                    }
                    catch ( InterruptedException e )
                    {
                        throw new Error(e);
                    }
                }

                @Override
                public void cool()
                {
                    super.cool();
                    coolLatch.countDown();
                    try
                    {
                        coolContinueLatch.await();
                    }
                    catch ( InterruptedException e )
                    {
                        throw new Error(e);
                    }
                }
            };
            manager.add(obj);
        }
        manager.start();

        Assert.assertTrue(warmLatch.await(10, TimeUnit.SECONDS));
        Assert.assertEquals(ObjectWithParallelWarmUp.warmUpCount.get(), OBJECT_QTY);
        Assert.assertEquals(ObjectWithParallelWarmUp.coolDownCount.get(), 0);

        warmContinueLatch.countDown();
        manager.close();
        Assert.assertTrue(coolLatch.await(10, TimeUnit.SECONDS));
        Assert.assertEquals(ObjectWithParallelWarmUp.warmUpCount.get(), OBJECT_QTY);
        Assert.assertEquals(ObjectWithParallelWarmUp.coolDownCount.get(), OBJECT_QTY);

        coolContinueLatch.countDown();
    }
}
