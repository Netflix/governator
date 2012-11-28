package com.netflix.governator.guice.mocks;

import com.netflix.governator.annotations.AutoBindSingleton;

@AutoBindSingleton(SimpleGenericInterface.class)
public class AutoBindSingletonToGenericInterface implements SimpleGenericInterface<String>
{
    @Override
    public String getValue()
    {
        return "a is a";
    }
}
