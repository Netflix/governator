package com.netflix.governator.autobind;

import com.google.inject.Inject;
import com.netflix.governator.annotations.AutoBind;

public class SimpleWithFieldAutoBind
{
    @AutoBind("field1")
    @Inject
    public MockWithParameter    field1;

    @AutoBind("field2")
    @Inject
    public MockWithParameter    field2;
}
