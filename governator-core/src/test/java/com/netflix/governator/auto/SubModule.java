package com.netflix.governator.auto;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

public class SubModule extends AbstractModule {

    @Override
    protected void configure() {
        System.out.println("Installing SubModule");
    }
    
    @Provides
    public String getConstant() {
        return "abc";
    }

}
