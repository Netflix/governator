package com.netflix.governator.lifecycle.warmup;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.netflix.governator.lifecycle.LifecycleMethods;
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

    public DependencyNode buildTree()
    {
        DependencyNode root = new DependencyNode(new Object());

        for ( Object objectKey : dependencies.keySet() )
        {
            internalBuildTree(root, objectKey);
        }

        return root;
    }

    public Object getObject(Object key)
    {
        return keyToObject.get(key);
    }

    public LifecycleMethods getLifecycleMethods(Object key)
    {
        return keyToLifecycle.get(key);
    }

    public void        clear()
    {
        keyToObject.clear();
        keyToLifecycle.clear();
        dependencies.clear();
    }

    private void internalBuildTree(DependencyNode parent, Object objectKey)
    {
        DependencyNode      node = new DependencyNode(objectKey);
        parent.addChild(node);

        for ( Object key : dependencies.get(objectKey) )
        {
            internalBuildTree(node, key);
        }
    }
}
