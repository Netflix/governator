package com.netflix.governator.lifecycle;

import com.netflix.governator.annotations.CoolDown;
import com.netflix.governator.lifecycle.mocks.ObjectWithWarmUp;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class TestWarmUp
{
    @Test
    public void     testStuckCoolDown() throws Exception
    {
        final CountDownLatch    latch = new CountDownLatch(1);
        Object                  obj = new Object()
        {
            @CoolDown
            public void     myCoolDown()
            {
                try
                {
                    latch.await();
                }
                catch ( InterruptedException e )
                {
                    Thread.currentThread().interrupt();
                    throw new Error("interrupted");
                }
            }
        };
        LifecycleManager        manager = new LifecycleManager();
        manager.add(obj);
        manager.start();

        final CountDownLatch    errorLatch = new CountDownLatch(1);
        manager.setListener
        (
            new LifecycleListener()
            {
                @Override
                public void objectInjected(Object obj)
                {
                }

                @Override
                public void stateChanged(Object obj, LifecycleState newState)
                {
                    if ( newState == LifecycleState.ERROR )
                    {
                        errorLatch.countDown();
                    }
                }
            }
        );

        manager.setMaxCoolDownMs(1);
        manager.close();

        Assert.assertTrue(errorLatch.await(10, TimeUnit.SECONDS));
        latch.countDown();
    }

    @Test
    public void     testMaxThreads() throws Exception
    {
        ObjectWithWarmUp.reset();

        final int               OBJECT_QTY = 10;
        LifecycleManager        manager = new LifecycleManager()
        {
            protected int getWarmUpThreadQty()
            {
                return 2;
            }
        };

        final Semaphore         warmSemaphore = new Semaphore(0);
        final Semaphore         coolSemaphore = new Semaphore(0);
        manager.setListener
        (
            new LifecycleListener()
            {
                @Override
                public void objectInjected(Object obj)
                {
                }

                @Override
                public void stateChanged(Object obj, LifecycleState newState)
                {
                    if ( newState == LifecycleState.ACTIVE )
                    {
                        warmSemaphore.release();
                    }
                    else if ( newState == LifecycleState.PRE_DESTROYING )
                    {
                        coolSemaphore.release();
                    }
                }
            }
        );
        for ( int i = 0; i < OBJECT_QTY; ++i )
        {
            Object      obj = new ObjectWithWarmUp()
            {
                @Override
                public void warm()
                {
                    super.warm();

                    try
                    {
                        Thread.sleep((int)(10 * Math.random()) + 1);
                    }
                    catch ( InterruptedException e )
                    {
                        Thread.currentThread().interrupt();
                    }
                }

                @Override
                public void cool()
                {
                    super.cool();

                    try
                    {
                        Thread.sleep((int)(10 * Math.random()) + 1);
                    }
                    catch ( InterruptedException e )
                    {
                        Thread.currentThread().interrupt();
                    }
                }
            };
            manager.add(obj);
        }
        manager.start();

        Assert.assertTrue(warmSemaphore.tryAcquire(OBJECT_QTY, 10, TimeUnit.SECONDS));
        Assert.assertEquals(ObjectWithWarmUp.warmUpCount.get(), OBJECT_QTY);
        Assert.assertEquals(ObjectWithWarmUp.coolDownCount.get(), 0);

        manager.close();

        Assert.assertTrue(coolSemaphore.tryAcquire(OBJECT_QTY, 10, TimeUnit.SECONDS));
        Assert.assertEquals(ObjectWithWarmUp.warmUpCount.get(), OBJECT_QTY);
        Assert.assertEquals(ObjectWithWarmUp.coolDownCount.get(), OBJECT_QTY);
    }

    @Test
    public void     testSimple() throws Exception
    {
        ObjectWithWarmUp.reset();

        final CountDownLatch          warmLatch = new CountDownLatch(1);
        final CountDownLatch          coolLatch = new CountDownLatch(1);
        ObjectWithWarmUp obj = new ObjectWithWarmUp()
        {
            @Override
            public void warm()
            {
                super.warm();
                warmLatch.countDown();
            }

            @Override
            public void cool()
            {
                super.cool();
                coolLatch.countDown();
            }
        };
        LifecycleManager        manager = new LifecycleManager();
        manager.add(obj);
        manager.start();

        Assert.assertTrue(warmLatch.await(10, TimeUnit.SECONDS));
        Assert.assertEquals(ObjectWithWarmUp.warmUpCount.get(), 1);
        Assert.assertEquals(ObjectWithWarmUp.coolDownCount.get(), 0);

        manager.close();

        Assert.assertTrue(coolLatch.await(10, TimeUnit.SECONDS));
        Assert.assertEquals(ObjectWithWarmUp.warmUpCount.get(), 1);
        Assert.assertEquals(ObjectWithWarmUp.coolDownCount.get(), 1);
    }

    @Test
    public void     testParallel() throws Exception
    {
        ObjectWithWarmUp.reset();

        final int               OBJECT_QTY = 10;

        final CountDownLatch    warmLatch = new CountDownLatch(OBJECT_QTY);
        final CountDownLatch    warmContinueLatch = new CountDownLatch(1);
        final CountDownLatch    coolLatch = new CountDownLatch(OBJECT_QTY);
        LifecycleManager        manager = new LifecycleManager()
        {
            protected int getWarmUpThreadQty()
            {
                return OBJECT_QTY;
            }
        };
        for ( int i = 0; i < OBJECT_QTY; ++i )
        {
            Object      obj = new ObjectWithWarmUp()
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
                }
            };
            manager.add(obj);
        }
        manager.start();

        Assert.assertTrue(warmLatch.await(10, TimeUnit.SECONDS));
        Assert.assertEquals(ObjectWithWarmUp.warmUpCount.get(), OBJECT_QTY);
        Assert.assertEquals(ObjectWithWarmUp.coolDownCount.get(), 0);

        warmContinueLatch.countDown();
        manager.close();
        Assert.assertTrue(coolLatch.await(10, TimeUnit.SECONDS));
        Assert.assertEquals(ObjectWithWarmUp.warmUpCount.get(), OBJECT_QTY);
        Assert.assertEquals(ObjectWithWarmUp.coolDownCount.get(), OBJECT_QTY);
    }
}
