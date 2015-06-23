package com.netflix.governator.autobindmultiple.basic;

import com.netflix.governator.annotations.AutoBindSingleton;

@AutoBindSingleton(multiple = true, baseClass = BaseForMocks.class)
public class MockC implements BaseForMocks
{
    @Override
    public String getValue()
    {
        return "C";
    }
}
