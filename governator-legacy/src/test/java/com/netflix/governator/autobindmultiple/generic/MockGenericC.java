package com.netflix.governator.autobindmultiple.generic;

import com.netflix.governator.annotations.AutoBindSingleton;

@AutoBindSingleton(multiple = true, baseClass = BaseForGenericMocks.class)
public class MockGenericC implements BaseForGenericMocks<Integer>
{
    @Override
    public Integer getValue()
    {
        return 3;
    }
}
