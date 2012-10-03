package com.netflix.governator.autobind;

import com.google.inject.Inject;
import com.netflix.governator.annotations.AutoBind;
import com.netflix.governator.annotations.HasAutoBind;

public class SimpleWithMethodAutoBind
{
    private MockWithParameter       f1;

    private MockWithParameter       f2;

    public MockWithParameter getF1()
    {
        return f1;
    }

    @HasAutoBind
    @Inject
    public void setF1(@AutoBind("f1") MockWithParameter f1)
    {
        this.f1 = f1;
    }

    public MockWithParameter getF2()
    {
        return f2;
    }

    @HasAutoBind
    @Inject
    public void setF2(@AutoBind("f2") MockWithParameter f2)
    {
        this.f2 = f2;
    }
}
