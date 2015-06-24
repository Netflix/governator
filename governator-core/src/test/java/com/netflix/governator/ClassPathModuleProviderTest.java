package com.netflix.governator;

import junit.framework.Assert;

import org.testng.annotations.Test;

import com.netflix.governator.auto.ClassPathModuleProvider;

public class ClassPathModuleProviderTest {
    @Test
    public void scanExistingClassPath() {
        ClassPathModuleProvider provider = new ClassPathModuleProvider("com.netflix.governator");
        Assert.assertFalse(provider.get().isEmpty());
    }
}
