package com.netflix.governator.lifecycle.warmup;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.governator.annotations.WarmUp;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.LifecycleMethods;
import com.netflix.governator.lifecycle.LifecycleState;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WarmUpManager
{
    private final LifecycleManager lifecycleManager;
    private final SetStateMixin setState;
    private final int nThreads;

    // guarded by synchronization
    private long updateCount = 0;

    public WarmUpManager(LifecycleManager lifecycleManager, SetStateMixin setState, int nThreads)
    {
        this.lifecycleManager = lifecycleManager;
        this.setState = setState;
        this.nThreads = nThreads;
    }

    public void warmUp() throws InterruptedException
    {
        DependencyNode      root = lifecycleManager.getDAGManager().buildTree();
        ExecutorService     service = Executors.newFixedThreadPool(nThreads, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("GovernatorWarmUpManager-%d").build());
        try
        {
            for(;;)
            {
                long        localUpdateCount = getUpdateCount();
                if ( !internalIterator(root, service) )
                {
                    break;
                }
                waitForUpdateCountChange(localUpdateCount);
            }
        }
        finally
        {
            service.shutdownNow();
        }
    }

    private boolean internalIterator(DependencyNode node, ExecutorService service)
    {
        boolean     result = false;
        if ( (getNodeState(node) == LifecycleState.PRE_WARMING_UP) && isReadyToWarmUp(node) )
        {
            Object                  obj = lifecycleManager.getDAGManager().getObject(node.getKey());
            if ( obj == null )
            {
                // TODO
            }
            LifecycleMethods        lifecycleMethods = lifecycleManager.getDAGManager().getLifecycleMethods(node.getKey());
            if ( lifecycleMethods != null )
            {
                Collection<Method>  methods = lifecycleMethods.methodsFor(WarmUp.class);
                warmupObject(service, obj, methods);
                result = true;
            }
        }

        for ( DependencyNode child : node.getChildren() )
        {
            boolean     recurseResult = internalIterator(child, service);
            result = result || recurseResult;
        }
        return result;
    }

    private boolean isReadyToWarmUp(DependencyNode node)
    {
        for ( DependencyNode child : node.getChildren() )
        {
            LifecycleState childState = getNodeState(child);
            if ( (childState == LifecycleState.PRE_WARMING_UP) || (childState == LifecycleState.WARMING_UP) )
            {
                // The original node has a direct child that is not warmed up
                return false;
            }
            if ( !isReadyToWarmUp(child) )
            {
                // The original node has an indirect child that is not warmed up
                return false;
            }
        }
        // Since no direct or indirect children are not warm,
        // this node is ready to warm up.
        return true;
    }

    private void warmupObject(ExecutorService service, final Object obj, final Collection<Method> warmupMethodsForObject)
    {
        if ( (warmupMethodsForObject == null) || (warmupMethodsForObject.size() == 0) )
        {
            changeState(obj, LifecycleState.ACTIVE);
            return;
        }

        // TODO - enforce a max time for object to warmUp

        setState.setState(obj, LifecycleState.WARMING_UP);
        service.submit
        (
            new Runnable()
            {
                @Override
                public void run()
                {
                    try
                    {
                        for ( Method method : warmupMethodsForObject )
                        {
                            method.invoke(obj);
                        }
                        changeState(obj, LifecycleState.ACTIVE);
                    }
                    catch ( Throwable e )
                    {
                        // TODO
                        changeState(obj, LifecycleState.ERROR);
                    }
                }
            }
        );
    }

    private synchronized void changeState(Object obj, LifecycleState state)
    {
        setState.setState(obj, state);
        ++updateCount;
        notifyAll();
    }

    private synchronized long getUpdateCount()
    {
        return updateCount;
    }

    private synchronized void waitForUpdateCountChange(long localUpdateCount) throws InterruptedException
    {
        // TODO max wait
        while ( localUpdateCount == updateCount )
        {
            wait();
        }
    }

    private LifecycleState getNodeState(DependencyNode node)
    {
        Object obj = lifecycleManager.getDAGManager().getObject(node.getKey());
        return (obj != null) ? lifecycleManager.getState(obj) : LifecycleState.LATENT;
    }
}
