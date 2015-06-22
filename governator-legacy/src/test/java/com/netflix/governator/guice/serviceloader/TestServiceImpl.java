package com.netflix.governator.guice.serviceloader;

import javax.inject.Inject;

public class TestServiceImpl implements TestService {
    private Boolean injected = false;
    
    public TestServiceImpl() {
        System.out.println("TestServiceImpl");
    }
    
    @Inject
    public void setInjected(Boolean injected) {
        this.injected = injected;

    }

    @Override
    public boolean isInjected() {
        return injected;
    }
}
