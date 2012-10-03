package com.netflix.governator.autobind;

import com.google.inject.Inject;
import com.netflix.governator.annotations.AutoBind;
import com.netflix.governator.annotations.HasAutoBind;

public class SimpleAutoBind
{
    private final String aString;

    @Inject
    @HasAutoBind
    public SimpleAutoBind(@AutoBind("foo") String aString)
    {
        this.aString = aString;
    }

    public String getString()
    {
        return aString;
    }
}
