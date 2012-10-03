package com.netflix.governator.autobind;

import com.google.inject.Inject;
import com.netflix.governator.annotations.AutoBind;
import com.netflix.governator.annotations.HasAutoBind;

public class SimpleWithMultipleAutoBinds
{
    private final MockWithParameter arg1;
    private final MockWithParameter arg2;
    private final MockWithParameter arg3;
    private final MockWithParameter arg4;

    @Inject
    @HasAutoBind
    public SimpleWithMultipleAutoBinds
        (
            @AutoBind("one") MockWithParameter arg1,
            @AutoBind("two") MockWithParameter arg2,
            @AutoBind("three") MockWithParameter arg3,
            @AutoBind("four") MockWithParameter arg4
        )
    {
        this.arg1 = arg1;
        this.arg2 = arg2;
        this.arg3 = arg3;
        this.arg4 = arg4;
    }

    public MockWithParameter getArg1()
    {
        return arg1;
    }

    public MockWithParameter getArg2()
    {
        return arg2;
    }

    public MockWithParameter getArg3()
    {
        return arg3;
    }

    public MockWithParameter getArg4()
    {
        return arg4;
    }
}
