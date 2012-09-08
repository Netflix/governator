package com.netflix.governator.configuration;

import com.google.common.collect.Lists;
import java.util.List;

public class KeyParser
{
    /**
     * Parse a key into parts
     *
     * @param raw the key
     * @return parts
     */
    public static List<ConfigurationKeyPart>  parse(String raw)
    {
        List<ConfigurationKeyPart>     parts = Lists.newArrayList();

        int                             caret = 0;
        for(;;)
        {
            int     startIndex = raw.indexOf("${", caret);
            if ( startIndex < 0 )
            {
                break;
            }
            int     endIndex = raw.indexOf("}", startIndex);
            if ( endIndex < 0 )
            {
                break;
            }

            if ( startIndex > caret )
            {
                parts.add(new ConfigurationKeyPart(raw.substring(caret, startIndex), false));
            }
            startIndex += 2;
            if ( startIndex < endIndex )
            {
                parts.add(new ConfigurationKeyPart(raw.substring(startIndex, endIndex), true));
            }
            caret = endIndex + 1;
        }

        if ( caret < raw.length() )
        {
            parts.add(new ConfigurationKeyPart(raw.substring(caret), false));
        }

        if ( parts.size() == 0 )
        {
            parts.add(new ConfigurationKeyPart("", false));
        }

        return parts;
    }

    private KeyParser()
    {
    }
}
