package com.netflix.governator.lifecycle.warmup;

import java.util.LinkedList;
import java.util.List;

public class DependencyNode
{
    private final List<DependencyNode> children = new LinkedList<DependencyNode>();
    private final Object key;

    public DependencyNode(Object key)
    {
        this.key = key;
    }

    public void addChild(DependencyNode child)
    {
        children.add(child);
    }

    public Object getKey()
    {
        return key;
    }

    public List<DependencyNode> getChildren()
    {
        return children;
    }
}
