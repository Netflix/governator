package com.netflix.governator.configuration;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class ConfigurationKey
{
    private final Logger        log = LoggerFactory.getLogger(getClass());
    private final String        rawKey;
    private final List<Part>    parts;

    public static class Part
    {
        private final String        value;
        private final boolean       isVariable;

        public Part(String value, boolean variable)
        {
            this.value = value;
            isVariable = variable;
        }

        public String getValue()
        {
            return value;
        }

        public boolean isVariable()
        {
            return isVariable;
        }
    }

    public ConfigurationKey(String rawKey, List<Part> parts)
    {
        this.rawKey = rawKey;
        this.parts = ImmutableList.copyOf(parts);
    }

    public String getRawKey()
    {
        return rawKey;
    }

    public String getKey(Map<String, String> variableValues)
    {
        StringBuilder       key = new StringBuilder();
        for ( Part p : parts )
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

    public List<Part> getParts()
    {
        return parts;
    }

    public Collection<String> getVariableNames()
    {
        ImmutableSet.Builder<String> builder = ImmutableSet.builder();
        for ( Part p : parts )
        {
            if ( p.isVariable() )
            {
                builder.add(p.getValue());
            }
        }

        return builder.build();
    }
}
