package com.netflix.governator.auto.modules;

import com.google.inject.AbstractModule;
import com.netflix.governator.auto.annotations.ConditionalOnProfile;
import com.netflix.governator.auto.annotations.ConditionalOnProperty;

@ConditionalOnProperty(name="test", value="B")
@ConditionalOnProfile(value={"A"})
public class ProfileBModule extends AbstractModule {
    @Override
    protected void configure() {
        System.out.println("Installing ProfileBModule");
    }
}
