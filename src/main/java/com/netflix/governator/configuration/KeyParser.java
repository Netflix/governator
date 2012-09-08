package com.netflix.governator.configuration;

import com.google.common.collect.Lists;
import java.util.List;

public class KeyParser
{
    public static List<ConfigurationKey.Part>  parse(String raw)
    {
        List<ConfigurationKey.Part>     parts = Lists.newArrayList();

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
                parts.add(new ConfigurationKey.Part(raw.substring(caret, startIndex), false));
            }
            startIndex += 2;
            if ( startIndex < endIndex )
            {
                parts.add(new ConfigurationKey.Part(raw.substring(startIndex, endIndex), true));
            }
            caret = endIndex + 1;
        }

        if ( caret < raw.length() )
        {
            parts.add(new ConfigurationKey.Part(raw.substring(caret), false));
        }

        if ( parts.size() == 0 )
        {
            parts.add(new ConfigurationKey.Part("", false));
        }

        return parts;
    }

    private KeyParser()
    {
    }
}
