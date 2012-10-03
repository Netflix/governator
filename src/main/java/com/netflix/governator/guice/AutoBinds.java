package com.netflix.governator.guice;

import com.netflix.governator.annotations.AutoBind;

/**
 * Used to build an {@link AutoBind} instance. Normally you won't
 * use this directly.
 */
public class AutoBinds
{
    public static AutoBind withValue(String value)
    {
        return new AutoBindImpl(value);
    }

    private AutoBinds()
    {
    }
}
