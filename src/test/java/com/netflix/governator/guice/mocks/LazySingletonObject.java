package com.netflix.governator.guice.mocks;

import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

public class LazySingletonObject
{
    public static final AtomicInteger           constructorCount = new AtomicInteger(0);
    public static final AtomicInteger           postConstructCount = new AtomicInteger(0);

    public LazySingletonObject()
    {
        constructorCount.incrementAndGet();
    }

    @PostConstruct
    public void         postConstruct()
    {
        postConstructCount.incrementAndGet();
    }
}
