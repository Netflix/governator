package com.netflix.governator.autobind;

public class MockWithParameter
{
    private final String parameter;

    public MockWithParameter(String parameter)
    {
        this.parameter = parameter;
    }

    public String getParameter()
    {
        return parameter;
    }
}
