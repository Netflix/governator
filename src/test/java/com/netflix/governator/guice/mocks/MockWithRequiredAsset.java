package com.netflix.governator.guice.mocks;

import com.google.inject.Inject;
import com.netflix.governator.annotations.RequiredAsset;

@RequiredAsset("test")
public class MockWithRequiredAsset
{
    @Inject
    public MockWithRequiredAsset()
    {
    }
}
