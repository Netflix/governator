package com.netflix.governator.lifecycle.warmup;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.netflix.governator.annotations.WarmUp;
import com.netflix.governator.lifecycle.LifecycleManager;
import com.netflix.governator.lifecycle.LifecycleMethods;
import com.netflix.governator.lifecycle.LifecycleState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.RecursiveAction;

/**
 * <p>
 *     A Fork Join task to warm up objects. Each node's dependency's
 *     are warmed-up via forking before the node itself warms-up.
 * </p>
 *
 * <p>
 *     Thanks to Allan Pratt for his help and design ideas
 * </p>
 */
public class WarmUpTask extends RecursiveAction
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final LifecycleManager lifecycleManager;
    private final SetStateMixin setStateMixin;
    private final boolean isRoot;
    private final DependencyNode node;

    /**
     * @param node the node to warm up
     * @param lifecycleManager lifecycle manager
     * @param setStateMixin used to change object state
     * @param isRoot true if the node is the root node (don't try to warm it up)
     */
    public WarmUpTask(DependencyNode node, LifecycleManager lifecycleManager, SetStateMixin setStateMixin, boolean isRoot)
    {
        this.node = node;
        this.lifecycleManager = lifecycleManager;
        this.setStateMixin = setStateMixin;
        this.isRoot = isRoot;
    }

    @Override
    protected void compute()
    {
        List<WarmUpTask> childTasks = Lists.transform
        (
            node.getChildren(),
            new Function<DependencyNode, WarmUpTask>()
            {
                @Override
                public WarmUpTask apply(DependencyNode child)
                {
                    return new WarmUpTask(child, lifecycleManager, setStateMixin, false);
                }
            }
        );
        invokeAll(childTasks);

        if ( !isRoot )
        {
            warmUpObject();
        }
    }

    private void warmUpObject()
    {
        Object                  obj = lifecycleManager.getDAGManager().getObject(node.getKey());
        if ( obj == null )
        {
            log.error(String.format("Could not find lifecycle-registered object for key. Ignoring object. Key: %s - KeyClass: %s", node.getKey(), node.getKey().getClass().getName()));
        }
        else
        {
            Collection<Method>  methods = null;
            synchronized(lifecycleManager)
            {
                System.out.println("pre " + obj);
                if ( lifecycleManager.getState(obj) == LifecycleState.PRE_WARMING_UP )
                {
                    LifecycleMethods    lifecycleMethods = lifecycleManager.getDAGManager().getLifecycleMethods(node.getKey());
                    methods = (lifecycleMethods != null) ? lifecycleMethods.methodsFor(WarmUp.class) : null;
                    if ( (methods == null) || (methods.size() == 0) )
                    {
                        setStateMixin.setState(obj, LifecycleState.ACTIVE);
                    }
                    else
                    {
                        setStateMixin.setState(obj, LifecycleState.WARMING_UP);
                    }
                }
            }

            if ( (methods != null) && (methods.size() > 0) )
            {
                LifecycleState newState = LifecycleState.ACTIVE;
                try
                {
                    for ( Method method : methods )
                    {
                        method.invoke(obj);
                    }
                }
                catch ( Throwable e )
                {
                    log.error(String.format("Error warming up object. Object: (%s) - Object Class: (%s)", obj, obj.getClass().getName()), e);
                    newState = LifecycleState.ERROR;
                }
                finally
                {
                    System.out.println("post " + obj);
                    synchronized(lifecycleManager)
                    {
                        setStateMixin.setState(obj, newState);
                    }
                }
            }
        }
    }
}
