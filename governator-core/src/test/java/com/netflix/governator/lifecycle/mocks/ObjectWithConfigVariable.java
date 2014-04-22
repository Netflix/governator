package com.netflix.governator.lifecycle.mocks;

import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.ConfigurationVariable;

import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class ObjectWithConfigVariable {
    @ConfigurationVariable(name="name")
    private final String name;
    
    @Configuration(value = "${name}.b", documentation = "this is a boolean")
    public boolean aBool = false;

    @Configuration("${name}.i")
    public int anInt = 1;

    @Configuration("${name}.l")
    public long aLong = 2;

    @Configuration("${name}.d")
    public double aDouble = 3.4;

    @Configuration("${name}.s")
    public String aString = "test";

    @Configuration("${name}.dt")
    public Date aDate = null;

    @Configuration(value = "${name}.obj")
    public List<Integer> ints = Arrays.asList(5, 6, 7);

    public ObjectWithConfigVariable(String name) {
        this.name = name;
    }
}
