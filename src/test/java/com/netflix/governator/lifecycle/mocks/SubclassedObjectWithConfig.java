package com.netflix.governator.lifecycle.mocks;

import com.netflix.governator.annotations.Configuration;

public class SubclassedObjectWithConfig extends ObjectWithConfig
{
    @Configuration(value = "test.main", documentation = "This is the mainInt")
    public int      mainInt;
}
