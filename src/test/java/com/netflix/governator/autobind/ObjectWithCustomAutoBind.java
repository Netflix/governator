package com.netflix.governator.autobind;

import com.google.inject.Inject;
import com.netflix.governator.annotations.HasAutoBind;

public class ObjectWithCustomAutoBind
{
    private final MockInjectable injectable;

    @Inject
    @HasAutoBind
    public ObjectWithCustomAutoBind(@CustomAutoBind(str = "hey", value = 1234) MockInjectable injectable)
    {
        this.injectable = injectable;
    }

    public MockInjectable getInjectable()
    {
        return injectable;
    }
}
