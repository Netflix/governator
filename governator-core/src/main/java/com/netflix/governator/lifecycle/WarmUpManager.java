package com.netflix.governator.lifecycle;

import com.google.common.collect.Lists;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class WarmUpManager
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final List<List<Work>> parallelQueues = Lists.newArrayList();
    private final List<Work> foregroundQueue = Lists.newArrayList();
    private final LifecycleManager lifecycleManager;
    private final LifecycleState endState;

    private int     nextIndex = 0;  // 0 is reserved

    private static class Work
    {
        final Object obj;
        final Method method;

        private Work(Object obj, Method method)
        {
            this.obj = obj;
            this.method = method;
        }
    }

    public WarmUpManager(LifecycleManager lifecycleManager, LifecycleState endState, int nThreads)
    {
        this.lifecycleManager = lifecycleManager;
        this.endState = endState;

        for ( int i = 0; i < nThreads; ++i )
        {
            parallelQueues.add(Lists.<Work>newArrayList());
        }
    }

    public void     add(Object obj, Method method, boolean canBeParallel)
    {
        Work work = new Work(obj, method);
        if ( canBeParallel )
        {
            int             index = nextIndex++ % parallelQueues.size();
            parallelQueues.get(index).add(work);
        }
        else
        {
            foregroundQueue.add(work);
        }
    }

    public void     runAll() throws Exception
    {
        runAll(false, 0, null);
    }

    public void     runAll(boolean waitForParallel, long maxWait, TimeUnit unit) throws Exception
    {
        ExecutorService service = Executors.newFixedThreadPool(parallelQueues.size());
        startParallel(service);
        startForeground();

        if ( waitForParallel )
        {
            if ( !service.awaitTermination(maxWait, unit) )
            {
                log.warn("Parallel tasks timed out");
            }
        }
    }

    private void startForeground() throws WarmUpException
    {
        Throwable       exceptionChain = null;
        for ( Work work : foregroundQueue )
        {
            try
            {
                doWork(work);
            }
            catch ( Throwable e )
            {
                log.error("WarmUp failure", e);
                if ( exceptionChain == null )
                {
                    exceptionChain = e;
                }
                else
                {
                    exceptionChain = new Throwable(exceptionChain);
                }
            }
        }

        if ( exceptionChain != null )
        {
            throw new WarmUpException(exceptionChain);
        }
    }

    private void doWork(Work work) throws IllegalAccessException, InvocationTargetException
    {
        work.method.invoke(work.obj);
        lifecycleManager.setState(work.obj, endState);
    }

    private void startParallel(ExecutorService service)
    {
        for ( final List<Work> queue : parallelQueues )
        {
            service.submit
            (
                new Callable<Void>()
                {
                    @Override
                    public Void call()
                    {
                        for ( Work work : queue )
                        {
                            try
                            {
                                doWork(work);
                            }
                            catch ( Throwable e )
                            {
                                log.error("WarmUp failure", e);
                            }
                        }
                        return null;
                    }
                }
            );
        }
        service.shutdown();
    }
}
