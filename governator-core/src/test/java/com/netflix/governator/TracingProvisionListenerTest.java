package com.netflix.governator;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.netflix.governator.test.TracingProvisionListener;

import org.junit.Test;

public class TracingProvisionListenerTest {
    @Test
    public void testDefault() {
        LifecycleInjector injector = InjectorBuilder
            .fromModules(
                new AbstractModule() {
                    @Override
                    protected void configure() {
                        bindListener(Matchers.any(), TracingProvisionListener.createDefault());
                    }
                })
            .createInjector();
    }
}
