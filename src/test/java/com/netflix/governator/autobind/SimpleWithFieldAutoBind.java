package com.netflix.governator.autobind;

import com.google.inject.Inject;
import com.netflix.governator.annotations.AutoBind;
import com.netflix.governator.annotations.HasAutoBind;

public class SimpleWithFieldAutoBind
{
    @HasAutoBind
    @AutoBind("f1")
    @Inject
    public MockWithParameter       f1;

    @HasAutoBind
    @AutoBind("f2")
    @Inject
    public MockWithParameter       f2;
}
