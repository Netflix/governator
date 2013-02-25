package com.netflix.governator.lifecycle.mocks;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;
import com.netflix.governator.annotations.Configuration;

public class ObjectWithDynamicConfig {
    @Configuration(value = "test.dynamic.b", documentation = "this is a boolean")
    public Supplier<Boolean>   aDynamicBool = Suppliers.ofInstance(true);

    @Configuration(value = "test.dynamic.i")
    public Supplier<Integer>   anDynamicInt = Suppliers.ofInstance(1);

    @Configuration(value = "test.dynamic.l")
    public Supplier<Long>      aDynamicLong = Suppliers.ofInstance(2L);

    @Configuration(value = "test.dynamic.d")
    public Supplier<Double>    aDynamicDouble = Suppliers.ofInstance(3.4);
    
    @Configuration(value = "test.dynamic.s")
    public Supplier<String>    aDynamicString = Suppliers.ofInstance("a is a");
}
