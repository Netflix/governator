package com.netflix.governator.autobind;

import com.google.inject.Inject;

public class MockWithParameter
{
    private final String parameter;

    @Inject
    public MockWithParameter(String parameter)
    {
        this.parameter = parameter;
    }

    public String getParameter()
    {
        return parameter;
    }
}
