package com.netflix.governator.guice.modules;

import com.google.inject.Inject;
import com.netflix.governator.annotations.binding.Size;

public class ObjectB
{
    private final String size;

    @Inject
    public ObjectB(@Size String size)
    {
        this.size = size;
    }

    public String getSize()
    {
        return size;
    }
}
