package com.netflix.governator.lifecycle.warmup;

import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveAction;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Maps;

public class WarmUpSession
{
    private final ForkJoinPool forkJoinPool = new ForkJoinPool();
    private final WarmUpDriver warmUpDriver;
    private final DAGManager dagManager;

    // guarded by sync
    private boolean isPaddingInBackground = false;

    public WarmUpSession(WarmUpDriver warmUpDriver, DAGManager dagManager)
    {
        this.warmUpDriver = warmUpDriver;
        this.dagManager = dagManager;
    }

    public boolean doImmediate(long maxMs) throws Exception
    {
        warmUpDriver.setPreWarmUpState();

        WarmUpErrors                        errors = new WarmUpErrors();
        WarmUpTask                          rootTask = newRootTask(errors);

        forkJoinPool.submit(rootTask);
        forkJoinPool.shutdown();

        boolean                             success = forkJoinPool.awaitTermination(maxMs, TimeUnit.MILLISECONDS);
        if ( !success )
        {
            forkJoinPool.shutdownNow();
        }

        errors.throwIfErrors();

        warmUpDriver.setPostWarmUpState();

        return success;
    }

    public synchronized void doInBackground()
    {
        if ( !isPaddingInBackground )
        {
            isPaddingInBackground = true;
            forkJoinPool.submit(newBackgroundAction());
        }
    }

    private WarmUpTask newRootTask(WarmUpErrors errors)
    {
        DAGManager                          copy = dagManager.newCopyAndClear();
        ConcurrentMap<Object, WarmUpTask>   tasks = Maps.newConcurrentMap();
        return new WarmUpTask(warmUpDriver, errors, copy, tasks);
    }

    private RecursiveAction newBackgroundAction()
    {
        return new RecursiveAction()
        {
            @Override
            protected void compute()
            {
                try
                {
                    Thread.sleep(warmUpDriver.getPostStartArguments().getWarmUpPaddingMs());
                }
                catch ( InterruptedException e )
                {
                    Thread.currentThread().interrupt();
                    return;
                }

                WarmUpErrors                        errors;
                WarmUpTask                          rootTask;
                synchronized(WarmUpSession.this)
                {
                    errors = new WarmUpErrors();
                    rootTask = newRootTask(errors);
                    isPaddingInBackground = false;
                }

                rootTask.compute();

                try
                {
                    errors.throwIfErrors();
                }
                catch ( WarmUpException e )
                {
                    warmUpDriver.getPostStartArguments().getWarmUpErrorHandler().warmUpError(e);
                }

                warmUpDriver.setPostWarmUpState();
            }
        };
    }
}
