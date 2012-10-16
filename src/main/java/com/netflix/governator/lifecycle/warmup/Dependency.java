package com.netflix.governator.lifecycle.warmup;

class Dependency
{
    private final String name;
    private volatile WarmupState state;
    private final Object object;

    Dependency(Object n, WarmupState s) {
        object = n;
        name = n.getClass().getName();
        state = s;
    }

    Object getObject()
    {
        return object;
    }

    String getName()
    {
        return name;
    }

    WarmupState getState()
    {
        return state;
    }

    void setState(WarmupState state)
    {
        this.state = state;
    }
}
