/*
 * Copyright 2013 Netflix, Inc.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package com.netflix.governator.configuration;

/**
 * A portion of a configuration name
 */
public class ConfigurationKeyPart
{
    private final String value;
    private final boolean isVariable;

    /**
     * @param value    the string or variable name
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
