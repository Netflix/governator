package com.netflix.governator.guice.actions;

import com.google.inject.Injector;
import com.netflix.governator.guice.PostInjectorAction;
import com.netflix.governator.lifecycle.LifecycleManager;

public class LifecycleManagerStarter implements PostInjectorAction {
    private LifecycleManager manager;
    
    @Override
    public void call(Injector injector) {
        manager = injector.getInstance(LifecycleManager.class);
    }
    
    public void close() {
        if (manager != null) {
            manager.close();
        }
    }

}
