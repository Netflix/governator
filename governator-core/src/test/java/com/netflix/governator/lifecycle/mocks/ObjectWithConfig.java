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

package com.netflix.governator.lifecycle.mocks;

import com.netflix.governator.annotations.Configuration;

import java.util.*;

public class ObjectWithConfig
{
    @Configuration(value = "test.b", documentation = "this is a boolean")
    public boolean aBool = false;

    @Configuration("test.i")
    public int anInt = 1;

    @Configuration("test.l")
    public long aLong = 2;

    @Configuration("test.d")
    public double aDouble = 3.4;

    @Configuration("test.s")
    public String aString = "test";

    @Configuration("test.dt")
    public Date aDate = null;

    @Configuration(value = "test.obj")
    public List<Integer> ints = Arrays.asList(5,6,7);

    @Configuration(value = "test.mapOfMaps")
    public Map<String, Map<String, String>> mapOfMaps = new HashMap<String, Map<String, String>>();
}
