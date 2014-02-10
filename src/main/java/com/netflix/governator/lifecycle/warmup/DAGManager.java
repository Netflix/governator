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

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Sets;
import com.google.common.reflect.TypeToken;
import com.google.inject.TypeLiteral;
import com.netflix.governator.annotations.WarmUp;
import com.netflix.governator.lifecycle.LifecycleMethods;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Manages building of dependencies
 */
public class DAGManager
{
    private final Map<Object, Object> keyToObject = Maps.newHashMap();
    private final Map<Object, LifecycleMethods> keyToLifecycle = Maps.newHashMap();
    private final Multimap<Object, Object> dependencies = ArrayListMultimap.create();
    private final Set<Object> nonRoots = Sets.newHashSet();

    private static final Object ROOT_DEPENDENCY_KEY = new Object();

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

        for ( Object key : dependencies.keySet() )
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
     * @param object    the object
     * @param methods   the objects lifecycle methods
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
     * @param objectKey     object's key
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
        processDependencies();
        return generateTree();
    }

    public synchronized Object getObject(Object key)
    {
        return keyToObject.get(key);
    }

    public synchronized LifecycleMethods getLifecycleMethods(Object key)
    {
        return keyToLifecycle.get(key);
    }

    public synchronized void clear()
    {
        keyToObject.clear();
        keyToLifecycle.clear();
        dependencies.clear();
    }

    @VisibleForTesting
    DependencyNode generateTree()
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

    private DependencyNode internalBuildTree(DependencyNode parent, Object objectKey)
    {
        DependencyNode node = new DependencyNode(objectKey);
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

    private void processDependencies()
    {
        addImplicitDependencies();
        removeNonWarmUpDependencies();
        checkForCircularDependencies();
    }

    private void addImplicitDependencies()
    {
        Set<Object> objects = Sets.newHashSet();
        objects.addAll(dependencies.keySet());
        objects.addAll(dependencies.values());
        Set<List<Object>> pairs = Sets.cartesianProduct(objects, objects);
        for ( List<Object> pair : pairs )
        {
            addImplicitDependency(pair.get(0), pair.get(1));
        }
    }

    // This adds dependency entries indicating that an interface or superclass depends on its implementation(s).
    private void addImplicitDependency(final Object parent, final Object child)
    {
        if ( (parent instanceof TypeLiteral<?>) && (child instanceof TypeLiteral<?>) && !parent.equals(child) )
        {
            TypeLiteral<?> parentType = (TypeLiteral<?>)parent;
            TypeLiteral<?> childType = (TypeLiteral<?>)child;
            if ( TypeToken.of(parentType.getType()).isAssignableFrom(childType.getType()) )
            {
                dependencies.put(parent, child);
            }
        }
    }

    /*
     * The InternalLifecycleManager tells us about the dependencies in the entire object graph, but we're only concerned
     * about dependencies as the effect warmUp methods so we're going to remove entries related to classes without warm
     * up methods. We have to protect the dependencies of warm up methods though.
     */
    private void removeNonWarmUpDependencies()
    {
        for ( final Iterator<Object> it = dependencies.keySet().iterator(); it.hasNext(); )
        {
            final Object key = it.next();
            if ( !hasWarmUpMethod(key) )
            {
                if ( extractDependency(key) )
                {
                    it.remove();
                }
            }
        }
        // we already removed anything wihout warmups from the keys, so this won't break any dependencies,
        // just shrink the dependency map
        for ( final Iterator<Object> it = dependencies.values().iterator(); it.hasNext(); )
        {
            final Object value = it.next();
            if ( (!hasWarmUpMethod(value) && ROOT_DEPENDENCY_KEY != value) || dependencies.get(value).contains(value) )
            {
                it.remove();
            }
        }
    }

    private boolean extractDependency(final Object key)
    {
        boolean dependenciesPreserved = false;
        final Collection<Object> values = dependencies.get(key);
        for ( final Map.Entry<Object, Collection<Object>> entry : dependencies.asMap().entrySet() )
        {
            if ( entry.getValue().contains(key) )
            {
                dependenciesPreserved = true;
                entry.getValue().addAll(values);
            }
        }
        return dependenciesPreserved;
    }

    private boolean hasWarmUpMethod(final Object bridge)
    {
        final LifecycleMethods lifecycleMethods = keyToLifecycle.get(bridge);
        return null != lifecycleMethods && !lifecycleMethods.methodsFor(WarmUp.class).isEmpty();
    }

    /*
     * With the addition of implicit dependencies, circular dependencies are now possible.
     */
    private void checkForCircularDependencies()
    {
        final Set<Object> couldBeCircular = Sets.newHashSet(dependencies.keySet());
        couldBeCircular.retainAll(dependencies.values());
        checkForCircularDependencies(Sets.newLinkedHashSet(), couldBeCircular);
    }

    private void checkForCircularDependencies(final Set<Object> seen, final Collection<Object> objects)
    {
        for ( final Object object : objects )
        {
            if ( !seen.add(object) )
            {
                if ( hasWarmUpMethod(object) )
                {
                    throw new IllegalStateException("Circular dependency detected: " + seen + " -> " + object);
                }
            }
            checkForCircularDependencies(seen, dependencies.get(object));
            seen.remove(object);
        }
    }
}
