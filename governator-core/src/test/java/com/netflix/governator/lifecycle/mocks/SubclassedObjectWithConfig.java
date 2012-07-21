package com.netflix.governator.lifecycle.mocks;

import com.netflix.governator.annotations.Configuration;

public class SubclassedObjectWithConfig extends ObjectWithConfig
{
    @Configuration("test.main")
    public int      mainInt;
}
