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

import com.google.common.collect.Sets;
import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Set;

public class TestDAGManager
{
    @Test
    public void     testSingleRoot()
    {
        DAGManager      manager = new DAGManager();
        manager.addDependency("A", "B");
        manager.addDependency("A", "C");

        DependencyNode  root = manager.generateTree();
        Assert.assertEquals(root.getChildren().size(), 1);

        DependencyNode  aNode = root.getChildren().get(0);
        Assert.assertEquals(aNode.getKey(), "A");
        Assert.assertEquals(aNode.getChildren().size(), 2);

        Set<Object>     set = Sets.newHashSet();
        set.add(aNode.getChildren().get(0).getKey());
        set.add(aNode.getChildren().get(1).getKey());
        Assert.assertEquals(set, Sets.newHashSet("B", "C"));
    }

    @Test
    public void     testSingleRootDepsBothDirections()
    {
        DAGManager      manager = new DAGManager();
        manager.addDependency("A", "B");
        manager.addDependency("A", "C");
        manager.addDependency("B", "C");

        DependencyNode  root = manager.generateTree();
        Assert.assertEquals(root.getChildren().size(), 1);

        DependencyNode  aNode = root.getChildren().get(0);
        Assert.assertEquals(aNode.getKey(), "A");
        Assert.assertEquals(aNode.getChildren().size(), 2);
        Assert.assertEquals(aNode.getChildren().get(0).getKey(), "B");
        Assert.assertEquals(aNode.getChildren().get(1).getKey(), "C");
    }

    @Test
    public void     testMultiRoots()
    {
        DAGManager      manager = new DAGManager();
        manager.addDependency("A", "C");
        manager.addDependency("B", "C");

        DependencyNode  root = manager.generateTree();
        Assert.assertEquals(root.getChildren().size(), 2);

        Set<Object>     set = Sets.newHashSet();
        set.add(root.getChildren().get(0).getKey());
        set.add(root.getChildren().get(1).getKey());
        Assert.assertEquals(set, Sets.newHashSet("A", "B"));

        Assert.assertEquals(root.getChildren().get(0).getChildren().size(), 1);
        Assert.assertEquals(root.getChildren().get(0).getChildren().get(0).getKey(), "C");

        Assert.assertEquals(root.getChildren().get(1).getChildren().size(), 1);
        Assert.assertEquals(root.getChildren().get(1).getChildren().get(0).getKey(), "C");
    }

    @Test
    public void     testMultiRootsDepsBothDirections()
    {
        DAGManager      manager = new DAGManager();
        manager.addDependency("A", "C");
        manager.addDependency("C", "D");
        manager.addDependency("B", "D");

        DependencyNode  root = manager.generateTree();
        Assert.assertEquals(root.getChildren().size(), 2);

        Set<Object>     set = Sets.newHashSet();
        set.add(root.getChildren().get(0).getKey());
        set.add(root.getChildren().get(1).getKey());
        Assert.assertEquals(set, Sets.newHashSet("A", "B"));

        Assert.assertEquals(root.getChildren().get(0).getChildren().size(), 1);
        Assert.assertEquals(root.getChildren().get(1).getChildren().size(), 1);

        Object child0ChildKey = root.getChildren().get(0).getChildren().get(0).getKey();
        Object child1ChildKey = root.getChildren().get(1).getChildren().get(0).getKey();

        Assert.assertTrue(child0ChildKey.equals("C") || child0ChildKey.equals("D"), child0ChildKey.toString());
        Assert.assertTrue(child1ChildKey.equals("C") || child1ChildKey.equals("D"), child1ChildKey.toString());

        int         nextIndex = child0ChildKey.equals("C") ? 0 : 1;
        Assert.assertEquals(root.getChildren().get(nextIndex).getChildren().get(0).getChildren().size(), 1);
        Assert.assertEquals(root.getChildren().get(nextIndex).getChildren().get(0).getChildren().get(0).getKey(), "D");
    }
}
