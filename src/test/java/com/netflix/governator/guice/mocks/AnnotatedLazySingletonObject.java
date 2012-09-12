package com.netflix.governator.guice.mocks;

import com.netflix.governator.guice.lazy.LazySingleton;
import javax.annotation.PostConstruct;
import java.util.concurrent.atomic.AtomicInteger;

@LazySingleton
public class AnnotatedLazySingletonObject
{
    public static final AtomicInteger           constructorCount = new AtomicInteger(0);
    public static final AtomicInteger           postConstructCount = new AtomicInteger(0);

    public AnnotatedLazySingletonObject()
    {
        constructorCount.incrementAndGet();
    }

    @PostConstruct
    public void         postConstruct()
    {
        postConstructCount.incrementAndGet();
    }
}
