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

package configuration;

import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.PreConfiguration;

public class ExampleObject
{
    @Configuration("${prefix}.a-string")
    private String      aString = "default value";

    @Configuration("${prefix}.an-int")
    private int         anInt = 0;

    @Configuration("${prefix}.a-double")
    private double      aDouble = 0;

    @PreConfiguration
    public void     preConfig()
    {
        System.out.println("preConfig");
    }

    public String getAString()
    {
        return aString;
    }

    public int getAnInt()
    {
        return anInt;
    }

    public double getADouble()
    {
        return aDouble;
    }

    public void setAString(String aString)
    {
        this.aString = aString;
    }

    public void setAnInt(int anInt)
    {
        this.anInt = anInt;
    }

    public void setADouble(double aDouble)
    {
        this.aDouble = aDouble;
    }
}
