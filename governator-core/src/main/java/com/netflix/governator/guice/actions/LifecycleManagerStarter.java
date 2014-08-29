package com.netflix.governator.guice.actions;

import com.google.inject.Injector;
import com.netflix.governator.guice.PostInjectorAction;
import com.netflix.governator.lifecycle.LifecycleManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LifecycleManagerStarter implements PostInjectorAction {
    private static final Logger log = LoggerFactory.getLogger(LifecycleManagerStarter.class);

    @Override
    public void call(Injector injector) {
        LifecycleManager manager = injector.getInstance(LifecycleManager.class);
        try {
            manager.start();
        } catch (Exception e) {
            log.error("Failed to start LifecycleManager", e);
        }
    }
}
