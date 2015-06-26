package com.netflix.governator.auto.modules;

import com.google.inject.AbstractModule;

public class AModule extends AbstractModule {

    @Override
    protected void configure() {
        System.out.println("Installing AModule");
        
        install(new SubModule());
    }

}
