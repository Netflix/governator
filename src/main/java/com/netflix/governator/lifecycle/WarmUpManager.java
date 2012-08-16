package com.netflix.governator.lifecycle;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

class WarmUpManager
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final List<Work> workQueue = Lists.newArrayList();
    private final LifecycleManager lifecycleManager;
    private final LifecycleState endState;
    private final int nThreads;

    private class Work implements Callable<Void>
    {
        final Object obj;
        final Method method;

        @Override
        public Void call() throws Exception
        {
            try
            {
                method.invoke(obj);
                lifecycleManager.setState(obj, endState);
            }
            catch ( Throwable e )
            {
                log.error("WarmUp failure", e);
                lifecycleManager.setState(obj, LifecycleState.ERROR);
            }
            return null;
        }

        private Work(Object obj, Method method)
        {
            this.obj = obj;
            this.method = method;
        }
    }

    WarmUpManager(LifecycleManager lifecycleManager, LifecycleState endState, int nThreads)
    {
        this.lifecycleManager = lifecycleManager;
        this.endState = endState;
        this.nThreads = nThreads;
    }

    void     add(Object obj, Method method)
    {
        workQueue.add(new Work(obj, method));
    }

    void     runAll() throws Exception
    {
        internalRun(false, 0, null);
    }

    boolean     runAllAndWait(long wait, TimeUnit unit) throws Exception
    {
        return internalRun(true, wait, unit);
    }

    private boolean     internalRun(boolean doWait, long wait, TimeUnit unit) throws Exception
    {
        if ( workQueue.size() == 0 )
        {
            return true;
        }

        long                    startMs = System.currentTimeMillis();

        ExecutorService         service = Executors.newFixedThreadPool(nThreads, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("GovernatorWarmUpManager-%d").build());
        List<Future<Void>>      futures = Lists.newArrayList();
        for ( Work worker : workQueue )
        {
            futures.add(service.submit(worker));
        }
        service.shutdown();

        boolean         success = true;
        if ( doWait )
        {
            long        maxWaitMs = unit.toMillis(wait);

            for ( Future<Void> future : futures )
            {
                long        elapsedMs = System.currentTimeMillis() - startMs;
                long        thisWaitMs = maxWaitMs - elapsedMs;
                if ( thisWaitMs <= 0 )
                {
                    success = false;
                    break;
                }
                else
                {
                    try
                    {
                        future.get(thisWaitMs, TimeUnit.MILLISECONDS);
                    }
                    catch ( InterruptedException e )
                    {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                    catch ( TimeoutException e )
                    {
                        success = false;
                        break;
                    }
                }
            }

            if ( !success )
            {
                service.shutdownNow();
            }
        }

        return success;
    }
}
