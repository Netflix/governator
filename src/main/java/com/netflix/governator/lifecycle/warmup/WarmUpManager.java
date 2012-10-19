package com.netflix.governator.lifecycle.warmup;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.netflix.governator.annotations.WarmUp;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.LifecycleMethods;
import com.netflix.governator.lifecycle.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// thanks to Allan Pratt for the ideas and initial code
public class WarmUpManager
{
    private final Logger log = LoggerFactory.getLogger(getClass());
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

    public boolean warmUp(long maxMs) throws InterruptedException
    {
        boolean             success = true;
        long                startMs = System.currentTimeMillis();
        DependencyNode      root = lifecycleManager.getDAGManager().buildTree();
        ExecutorService     service = Executors.newFixedThreadPool(nThreads, new ThreadFactoryBuilder().setDaemon(true).setNameFormat("GovernatorWarmUpManager-%d").build());
        try
        {
            for(;;)
            {
                long        elapsed = System.currentTimeMillis() - startMs;
                long        thisWait = maxMs - elapsed;
                if ( thisWait <= 0 )
                {
                    success = false;
                    break;
                }

                long        localUpdateCount = getUpdateCount();
                if ( internalIterator(root, service) )
                {
                    break;
                }
                waitForUpdateCountChange(localUpdateCount, startMs, maxMs);
            }
        }
        finally
        {
            logIssues(root);
            service.shutdownNow();
        }

        return success;
    }

    private boolean internalIterator(DependencyNode node, ExecutorService service)
    {
        LifecycleState  nodeState = getNodeState(node);
        boolean         needsWarmup = (nodeState == LifecycleState.PRE_WARMING_UP);
        boolean         isDone = !needsWarmup && (nodeState != LifecycleState.WARMING_UP);

        if ( needsWarmup && isReadyToWarmUp(node) )
        {
            Object                  obj = lifecycleManager.getDAGManager().getObject(node.getKey());
            if ( obj == null )
            {
                log.error(String.format("Could not find lifecycle-registered object for key. Ignoring object. Key: %s - KeyClass: %s", node.getKey(), node.getKey().getClass().getName()));
            }
            else
            {
                LifecycleMethods        lifecycleMethods = lifecycleManager.getDAGManager().getLifecycleMethods(node.getKey());
                warmUpObject(service, obj, lifecycleMethods);
            }
        }

        for ( DependencyNode child : node.getChildren() )
        {
            if ( !internalIterator(child, service) )
            {
                isDone = false;
            }
        }
        return isDone;
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

    private void warmUpObject(ExecutorService service, final Object obj, LifecycleMethods lifecycleMethods)
    {
        final Collection<Method>  methods = (lifecycleMethods != null) ? lifecycleMethods.methodsFor(WarmUp.class) : null;
        if ( (methods == null) || (methods.size() == 0) )
        {
            changeState(obj, LifecycleState.ACTIVE);
            return;
        }

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
                        for ( Method method : methods )
                        {
                            method.invoke(obj);
                        }
                        changeState(obj, LifecycleState.ACTIVE);
                    }
                    catch ( Throwable e )
                    {
                        log.error(String.format("Error warming up object. Object: (%s) - Object Class: (%s)", obj, obj.getClass().getName()), e);
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

    private synchronized void waitForUpdateCountChange(long localUpdateCount, long startMs, long maxMs) throws InterruptedException
    {
        while ( localUpdateCount == updateCount )
        {
            long        elapsed = System.currentTimeMillis() - startMs;
            long        thisWait = maxMs - elapsed;
            if ( thisWait <= 0 )
            {
                break;
            }
            wait(thisWait);
        }
    }

    private LifecycleState getNodeState(DependencyNode node)
    {
        Object obj = lifecycleManager.getDAGManager().getObject(node.getKey());
        return (obj != null) ? lifecycleManager.getState(obj) : LifecycleState.LATENT;
    }

    private void logIssues(DependencyNode node)
    {
        LifecycleState      state = getNodeState(node);
        if ( (state != LifecycleState.ACTIVE) && (state != LifecycleState.LATENT) )
        {
            Object              obj = lifecycleManager.getDAGManager().getObject(node.getKey());
            log.error(String.format("Object did not complete warmup before timeout. Object: (%s) - Object Class: (%s)", obj, obj.getClass().getName()));
        }

        for ( DependencyNode child : node.getChildren() )
        {
            logIssues(child);
        }
    }
}
