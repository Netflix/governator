package com.netflix.governator.lifecycle.warmup;

import java.util.LinkedList;
import java.util.List;

class Tree
{
    private final List<Tree> children = new LinkedList<Tree>();
    private final Dependency data;

    Tree(Dependency data)
    {
        this.data = data;
    }

    void addChild(Tree child)
    {
        children.add(child);
    }

    Dependency getData()
    {
        return data;
    }

    List<Tree> getChildren()
    {
        return children;
    }
}
