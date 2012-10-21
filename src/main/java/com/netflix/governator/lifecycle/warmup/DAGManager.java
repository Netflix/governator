package com.netflix.governator.lifecycle.warmup;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.netflix.governator.lifecycle.LifecycleMethods;
import java.util.Map;
import java.util.Set;

/**
 * Manages building of dependencies
 */
public class DAGManager
{
    private final Map<Object, Object>               keyToObject = Maps.newHashMap();
    private final Map<Object, LifecycleMethods>     keyToLifecycle = Maps.newHashMap();
    private final Multimap<Object, Object>          dependencies = ArrayListMultimap.create();
    private final Set<Object>                       nonRoots = Sets.newHashSet();

    /**
     * Adds a mapping of an object "key" to an object
     *
     * @param objectKey the object's key
     * @param object the object
     * @param methods the objects lifecycle methods
     */
    public void addObjectMapping(Object objectKey, Object object, LifecycleMethods methods)
    {
        keyToObject.put(objectKey, object);
        keyToLifecycle.put(objectKey, methods);
    }

    /**
     * Adds a dependency for the given object
     *
     * @param objectKey object's key
     * @param dependencyKey object key of the dependency
     */
    public void addDependency(Object objectKey, Object dependencyKey)
    {
        dependencies.put(objectKey, dependencyKey);
        nonRoots.add(dependencyKey);
    }

    /**
     * Build the dependencies into a DAG
     *
     * @return root of the DAG
     */
    public DependencyNode buildTree()
    {
        DependencyNode root = new DependencyNode(new Object());
        for ( Object objectKey : dependencies.keySet() )
        {
            DependencyNode node = internalBuildTree(null, objectKey);
            if ( !nonRoots.contains(objectKey) )
            {
                root.addChild(node);
            }
        }

        Preconditions.checkState((root.getChildren().size() > 0) || (dependencies.size() == 0), "No root objects found. Maybe there are circular dependencies.");

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

    private DependencyNode internalBuildTree(DependencyNode parent, Object objectKey)
    {
        DependencyNode      node = new DependencyNode(objectKey);
        if ( parent != null )
        {
            parent.addChild(node);
        }

        for ( Object key : dependencies.get(objectKey) )
        {
            internalBuildTree(node, key);
        }

        return node;
    }
}
