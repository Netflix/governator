package com.netflix.governator.auto;

import javax.inject.Named;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.netflix.governator.auto.annotations.ConditionalOnProfile;
import com.netflix.governator.auto.annotations.OverrideModule;

@OverrideModule(SubModule.class)
@ConditionalOnProfile("test")
public class OverrideAModule extends AbstractModule {

    @Override
    protected void configure() {
        System.out.println("Installing ProfileAModule");
    }

    @Provides
    public String getConstant() {
        return "override";
    }
    
    @Named("OverrideAModule")
    @Provides
    public Boolean getIsInstalled() {
        return true;
    }
}
