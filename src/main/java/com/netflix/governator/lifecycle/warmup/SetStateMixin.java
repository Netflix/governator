package com.netflix.governator.lifecycle.warmup;

import com.netflix.governator.lifecycle.LifecycleState;

public interface SetStateMixin
{
    public void setState(Object obj, LifecycleState state);
}
