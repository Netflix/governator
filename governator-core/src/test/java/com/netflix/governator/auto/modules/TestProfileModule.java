package com.netflix.governator.auto.modules;

import javax.inject.Named;

import com.google.inject.Provides;
import com.netflix.governator.DefaultModule;

public class TestProfileModule extends DefaultModule {
    @Named("TestProfileModule")
    @Provides
    public Boolean getIsInstalled() {
        return true;
    }
}
