package com.netflix.governator.guice.mocks;

import com.google.inject.Inject;

public class ObjectWithGenericInterface
{
    private final SimpleGenericInterface<String> obj;

    @Inject
    public ObjectWithGenericInterface(SimpleGenericInterface<String> obj)
    {
        this.obj = obj;
    }

    public SimpleGenericInterface<String> getObj()
    {
        return obj;
    }
}
