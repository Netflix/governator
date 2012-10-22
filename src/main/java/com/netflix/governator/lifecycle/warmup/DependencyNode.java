package com.netflix.governator.lifecycle.warmup;

import java.util.LinkedList;
import java.util.List;

class DependencyNode
{
    private final List<DependencyNode> children = new LinkedList<DependencyNode>();
    private final Object key;

    DependencyNode(Object key)
    {
        this.key = key;
    }

    void addChild(DependencyNode child)
    {
        children.add(child);
    }

    Object getKey()
    {
        return key;
    }

    List<DependencyNode> getChildren()
    {
        return children;
    }
}
