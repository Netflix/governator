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

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.configuration.Property;

import java.util.*;

public class ObjectWithDynamicConfig
{
    @Configuration(value = "test.dynamic.b", documentation = "this is a boolean")
    public Supplier<Boolean> aDynamicBool = Suppliers.ofInstance(true);
    @Configuration(value = "test.dynamic.i")
    public Supplier<Integer> anDynamicInt = Suppliers.ofInstance(1);
    @Configuration(value = "test.dynamic.i")
    public Property<Integer> anDynamicInt2 = Property.from(1);
    @Configuration(value = "test.dynamic.l")
    public Supplier<Long> aDynamicLong = Suppliers.ofInstance(2L);
    @Configuration(value = "test.dynamic.d")
    public Supplier<Double> aDynamicDouble = Suppliers.ofInstance(3.4);
    @Configuration(value = "test.dynamic.s")
    public Supplier<String> aDynamicString = Suppliers.ofInstance("a is a");
    @Configuration(value = "test.dynamic.s")
    public Property<String> aDynamicString2 = Property.from("a is a");
    @Configuration(value = "test.dynamic.dt")
    public Supplier<Date> aDynamicDate = Suppliers.ofInstance(null);
    @Configuration(value = "test.dynamic.obj")
    public Supplier<List<Integer>> aDynamicObj = Suppliers.ofInstance(Arrays.asList(5, 6, 7));
    @Configuration(value = "test.dynamic.mapOfMaps")
    public Supplier<Map<String, Map<String, String>>> aDynamicMapOfMaps =
            Suppliers.ofInstance(Collections.<String, Map<String, String>>emptyMap());
}
