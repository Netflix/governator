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

import org.testng.Assert;
import org.testng.annotations.Test;
import java.util.Arrays;
import java.util.List;

public class TestKeyParser
{
    private static final String     VALUE = "value";
    private static final String     VARIABLE = "variable";

    private static String[][]       tests =
    {
        {"one two three", VALUE, "one two three"},
        {"one ${two} three", VALUE, "one ", VARIABLE, "two", VALUE, " three"},
        {"${two}", VARIABLE, "two"},
        {"${a}b", VARIABLE, "a", VALUE, "b"},
        {"a${b}", VALUE, "a", VARIABLE, "b"},
        {"", VALUE, ""},
        {"1 ${a} 2 ${b}", VALUE, "1 ", VARIABLE, "a", VALUE, " 2 ", VARIABLE, "b"},
        {"1 ${a} 2 ${b}${c}", VALUE, "1 ", VARIABLE, "a", VALUE, " 2 ", VARIABLE, "b", VARIABLE, "c"},
        {"${a}${b} one ${two} three", VARIABLE, "a", VARIABLE, "b", VALUE, " one ", VARIABLE, "two", VALUE, " three"},
        {"${a}${b}one${two}three", VARIABLE, "a", VARIABLE, "b", VALUE, "one", VARIABLE, "two", VALUE, "three"},
        {"${", VALUE, "${"},
        {"${foo bar", VALUE, "${foo bar"},
        {"${${ foo bar}", VARIABLE, "${ foo bar"},
    };

    @Test
    public void runTests()
    {
        for ( String[] spec : tests )
        {
            List<ConfigurationKeyPart> parts = KeyParser.parse(spec[0]);
            Assert.assertEquals(parts.size(), (spec.length - 1) / 2, Arrays.toString(spec));

            for ( int i = 1; (i + 1) < spec.length; i += 2 )
            {
                ConfigurationKeyPart thisPart = parts.get((i - 1) / 2);
                boolean                 isVariable = spec[i].equals(VARIABLE);
                Assert.assertEquals(isVariable, thisPart.isVariable(), Arrays.toString(spec) + " : index " + i);
                Assert.assertEquals(spec[i + 1], thisPart.getValue(), Arrays.toString(spec) + " : index " + i);
            }
        }
    }
}
