package com.netflix.governator.lifecycle.mocks;

import java.util.Date;

import com.netflix.governator.annotations.Configuration;
import com.netflix.governator.annotations.ConfigurationVariable;

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
    
    public ObjectWithConfigVariable(String name) {
        this.name = name;
    }
}
