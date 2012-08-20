package com.netflix.governator.guice.mocks;

import com.netflix.governator.annotations.Configuration;

public class ObjectWithConfig
{
    @Configuration("a")
    public int      a;

    @Configuration("b")
    public int      b;

    @Configuration("c")
    public int      c;

    public ObjectWithConfig()
    {
    }
}
