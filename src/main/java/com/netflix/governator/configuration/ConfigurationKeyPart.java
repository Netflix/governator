package com.netflix.governator.configuration;

/**
 * A portion of a configuration name
 */
public class ConfigurationKeyPart
{
    private final String        value;
    private final boolean       isVariable;

    /**
     * @param value the string or variable name
     * @param variable true if this is a variable substitution
     */
    public ConfigurationKeyPart(String value, boolean variable)
    {
        this.value = value;
        isVariable = variable;
    }

    /**
     * @return the name or variable name
     */
    public String getValue()
    {
        return value;
    }

    /**
     * @return true if this is a variable substitution
     */
    public boolean isVariable()
    {
        return isVariable;
    }
}
