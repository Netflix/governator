package com.netflix.governator.autobindmultiple.basic;

import com.netflix.governator.annotations.AutoBindSingleton;

@AutoBindSingleton(multiple = true, baseClass = BaseForMocks.class)
public class MockA implements BaseForMocks
{
    @Override
    public String getValue()
    {
        return "A";
    }
}
