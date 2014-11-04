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

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.configuration.Property;

public class ObjectWithDynamicConfig
{
    @Configuration(value = "test.dynamic.b", documentation = "this is a boolean")
    public Property<Boolean> aDynamicBool = Property.from(true);
    
    @Configuration(value = "test.dynamic.i")
    public Property<Integer> anDynamicInt = Property.from(1);
    
    @Configuration(value = "test.dynamic.i")
    public Supplier<Integer> anDynamicInt2 = Suppliers.ofInstance(1);
    
    @Configuration(value = "test.dynamic.l")
    public Property<Long> aDynamicLong = Property.from(2L);

    @Configuration(value = "test.dynamic.d")
    public Property<Double> aDynamicDouble = Property.from(3.4);
    
    @Configuration(value = "test.dynamic.s")
    public Property<String> aDynamicString = Property.from("a is a");
    
    @Configuration(value = "test.dynamic.s")
    public Supplier<String> aDynamicString2 = Suppliers.ofInstance("a is a");
    
    @Configuration(value = "test.dynamic.dt")
    public Property<Date> aDynamicDate = Property.from(null);
    
    @Configuration(value = "test.dynamic.obj")
    public Property<List<Integer>> aDynamicObj = Property.from(Arrays.asList(5, 6, 7));
    
    @Configuration(value = "test.dynamic.mapOfMaps")
    public Property<Map<String, Map<String, String>>> aDynamicMapOfMaps =
            Property.from(Collections.<String, Map<String, String>>emptyMap());
}
