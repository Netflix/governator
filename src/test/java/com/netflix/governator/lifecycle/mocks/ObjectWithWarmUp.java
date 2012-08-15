package com.netflix.governator.lifecycle.mocks;

import com.netflix.governator.annotations.CoolDown;
import com.netflix.governator.annotations.WarmUp;
import java.util.concurrent.atomic.AtomicInteger;

public class ObjectWithWarmUp
{
    public final AtomicInteger      warmUpCount = new AtomicInteger(0);
    public final AtomicInteger      coolDownCount = new AtomicInteger(0);

    @WarmUp
    public void     warm()
    {
        warmUpCount.incrementAndGet();
    }

    @CoolDown
    public void     cool()
    {
        coolDownCount.incrementAndGet();
    }
}
