package com.netflix.governator.lifecycle.mocks;

import com.netflix.governator.configuration.Configuration;

public class SubclassedObjectWithConfig extends ObjectWithConfig
{
    @Configuration("test.main")
    public int      mainInt;
}
