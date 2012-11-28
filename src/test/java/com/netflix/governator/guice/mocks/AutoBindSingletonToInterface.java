package com.netflix.governator.guice.mocks;

import com.netflix.governator.annotations.AutoBindSingleton;

@AutoBindSingleton(SimpleInterface.class)
public class AutoBindSingletonToInterface implements SimpleInterface
{
    @Override
    public int getValue()
    {
        return 1234;
    }
}
