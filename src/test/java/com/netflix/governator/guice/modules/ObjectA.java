package com.netflix.governator.guice.modules;

import com.google.inject.Inject;
import com.netflix.governator.annotations.binding.Color;

public class ObjectA
{
    private final String color;

    @Inject
    public ObjectA(@Color String color)
    {
        this.color = color;
    }

    public String getColor()
    {
        return color;
    }
}
