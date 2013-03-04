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
    public static List<ConfigurationKeyPart> parse(String raw)
    {
        List<ConfigurationKeyPart> parts = Lists.newArrayList();

        int caret = 0;
        for (; ; )
        {
            int startIndex = raw.indexOf("${", caret);
            if ( startIndex < 0 )
            {
                break;
            }
            int endIndex = raw.indexOf("}", startIndex);
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
