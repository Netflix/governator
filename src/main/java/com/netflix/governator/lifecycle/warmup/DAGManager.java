package com.netflix.governator.lifecycle.warmup;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.netflix.governator.annotations.WarmUp;
import com.netflix.governator.lifecycle.LifecycleMethods;
import java.util.Collection;
import java.util.Map;

public class DAGManager
{
    private final Map<Object, Object>               keyToObject = Maps.newHashMap();
    private final Map<Object, LifecycleMethods>     keyToLifecycle = Maps.newHashMap();
    private final Multimap<Object, Object>          dependencies = ArrayListMultimap.create();

    public void addObjectMapping(Object objectKey, Object object, LifecycleMethods methods)
    {
        keyToObject.put(objectKey, object);
        keyToLifecycle.put(objectKey, methods);
    }

    public void addDependency(Object objectKey, Object dependencyKey)
    {
        dependencies.put(objectKey, dependencyKey);
    }

    Tree        buildTree()
    {
        Tree            root = new Tree(new Dependency("", WarmupState.WARMED_UP));

        for ( Object object : dependencies.keySet() )
        {
            Object  objectKey = keyToObject.get(object);
            if ( objectKey == null )
            {
                // TODO
            }

            internalBuildTree(root, dependencies.get(objectKey));
        }

        return root;
    }

    void        clear()
    {
        keyToObject.clear();
        keyToLifecycle.clear();
        dependencies.clear();
    }

    private void internalBuildTree(Tree parent, Collection<Object> nodeDependencies)
    {
        for ( Object d : nodeDependencies )
        {
            LifecycleMethods    methods = keyToLifecycle.get(d);
            if ( methods == null )
            {
                // TODO
            }
            boolean             hasWarmups = (methods.methodsFor(WarmUp.class).size() > 0);

            Tree                child = new Tree(new Dependency(d, hasWarmups ? WarmupState.NOT_WARMED_UP : WarmupState.WARMED_UP));
            internalBuildTree(child, dependencies.get(d));
            parent.addChild(child);
        }
    }
}
