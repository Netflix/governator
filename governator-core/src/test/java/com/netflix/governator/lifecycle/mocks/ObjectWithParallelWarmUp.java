package com.netflix.governator.lifecycle.mocks;

import com.netflix.governator.annotations.CoolDown;
import com.netflix.governator.annotations.WarmUp;
import java.util.concurrent.atomic.AtomicInteger;

public class ObjectWithParallelWarmUp
{
    public static final AtomicInteger      warmUpCount = new AtomicInteger(0);
    public static final AtomicInteger      coolDownCount = new AtomicInteger(0);

    @WarmUp(canBeParallel = true)
    public void     warm()
    {
        warmUpCount.incrementAndGet();
    }

    @CoolDown(canBeParallel = true)
    public void     cool()
    {
        coolDownCount.incrementAndGet();
    }
}
