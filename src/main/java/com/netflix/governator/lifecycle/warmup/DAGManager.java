/*
 * Copyright 2013 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.governator.lifecycle.warmup;

import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.netflix.governator.lifecycle.LifecycleMethods;
import java.util.List;
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

    private static final Object         ROOT_DEPENDENCY_KEY = new Object();

    /**
     * Return a copy of this DAGManager and then clears this instance
     *
     * @return copy
     */
    public synchronized DAGManager newCopyAndClear()
    {
        DAGManager copy = new DAGManager();
        copy.keyToObject.putAll(keyToObject);
        copy.keyToLifecycle.putAll(keyToLifecycle);
        copy.nonRoots.addAll(nonRoots);

        for ( Object key : dependencies.keys() )
        {
            List<Object> objectsCopy = Lists.newArrayList(dependencies.get(key));
            copy.dependencies.putAll(key, objectsCopy);
        }

        clear();

        return copy;
    }

    /**
     * Adds a mapping of an object "key" to an object
     *
     * @param objectKey the object's key
     * @param object the object
     * @param methods the objects lifecycle methods
     */
    public synchronized void addObjectMapping(Object objectKey, Object object, LifecycleMethods methods)
    {
        keyToObject.put(objectKey, object);
        keyToLifecycle.put(objectKey, methods);
        dependencies.put(objectKey, ROOT_DEPENDENCY_KEY);   // add an initial entry in case this object has no dependencies
    }

    /**
     * Adds a dependency for the given object
     *
     * @param objectKey object's key
     * @param dependencyKey object key of the dependency
     */
    public synchronized void addDependency(Object objectKey, Object dependencyKey)
    {
        dependencies.put(objectKey, dependencyKey);
        nonRoots.add(dependencyKey);
    }

    /**
     * Build the dependencies into a DAG
     *
     * @return root of the DAG
     */
    public synchronized DependencyNode buildTree()
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

    public synchronized Object getObject(Object key)
    {
        return keyToObject.get(key);
    }

    public synchronized LifecycleMethods getLifecycleMethods(Object key)
    {
        return keyToLifecycle.get(key);
    }

    public synchronized void        clear()
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
