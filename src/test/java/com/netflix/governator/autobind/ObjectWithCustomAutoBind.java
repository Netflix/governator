package com.netflix.governator.autobind;

import com.google.inject.Inject;

public class ObjectWithCustomAutoBind
{
    private final MockInjectable injectable;

    @Inject
    public ObjectWithCustomAutoBind(@CustomAutoBind(str = "hey", value = 1234) MockInjectable injectable)
    {
        this.injectable = injectable;
    }

    public MockInjectable getInjectable()
    {
        return injectable;
    }
}
