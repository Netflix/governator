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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * Abstracts configuration names with variable replacements
 */
public class ConfigurationKey
{
    private final Logger log = LoggerFactory.getLogger(getClass());
    private final String rawKey;
    private final List<ConfigurationKeyPart> parts;

    /**
     * @param rawKey the unprocessed value
     * @param parts  the parsed values
     */
    public ConfigurationKey(String rawKey, List<ConfigurationKeyPart> parts)
    {
        this.rawKey = rawKey;
        this.parts = ImmutableList.copyOf(parts);
    }

    /**
     * @return the unprocessed key
     */
    public String getRawKey()
    {
        return rawKey;
    }

    /**
     * Return the final key applying variables as needed
     *
     * @param variableValues map of variable names to values
     * @return the key
     */
    public String getKey(Map<String, String> variableValues)
    {
        StringBuilder key = new StringBuilder();
        for ( ConfigurationKeyPart p : parts )
        {
            if ( p.isVariable() )
            {
                String value = variableValues.get(p.getValue());
                if ( value == null )
                {
                    log.warn("No value found for variable: " + p.getValue());
                    value = "";
                }
                key.append(value);
            }
            else
            {
                key.append(p.getValue());
            }
        }

        return key.toString();
    }

    /**
     * @return the parsed key parts
     */
    public List<ConfigurationKeyPart> getParts()
    {
        return parts;
    }

    /**
     * Return the names of the variables specified in the key if any
     *
     * @return names (might be zero sized)
     */
    public Collection<String> getVariableNames()
    {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for ( ConfigurationKeyPart p : parts )
        {
            if ( p.isVariable() )
            {
                builder.add(p.getValue());
            }
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return rawKey;
    }
}
