package com.netflix.governator.inject.guice;

import com.google.inject.Binder;

public interface BootstrapModule
{
    public void configure(Binder binder, RequiredAssetBinder requiredAssetBinder);
}
