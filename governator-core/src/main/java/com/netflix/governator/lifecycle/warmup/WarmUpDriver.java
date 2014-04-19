package com.netflix.governator.lifecycle.warmup;

import com.netflix.governator.lifecycle.LifecycleState;
import com.netflix.governator.lifecycle.PostStartArguments;

public interface WarmUpDriver
{
    public void setPreWarmUpState();

    public void setPostWarmUpState();

    public PostStartArguments getPostStartArguments();

    public void setState(Object obj, LifecycleState state);
}
