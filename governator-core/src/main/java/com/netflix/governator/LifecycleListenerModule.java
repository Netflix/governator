package com.netflix.governator;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.google.inject.AbstractModule;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.ProvisionListener;
import com.netflix.governator.annotations.SuppressLifecycleUninitialized;
import com.netflix.governator.spi.LifecycleListener;

/**
 * Adds support for detection and invocation of {@link LifecycleListener} instances. 
 */
public final class LifecycleListenerModule extends AbstractModule {
    
    private LifecycleListenerProvisionListener provisionListener = new LifecycleListenerProvisionListener();

    @Override
    protected void configure() {
        requestStaticInjection(LifecycleListenerProvisionListener.class);
        bind(LifecycleListenerProvisionListener.class).toInstance(provisionListener);
        bindListener(Matchers.any(), provisionListener);
    }
    
    @Singleton
    @SuppressLifecycleUninitialized
    static class LifecycleListenerProvisionListener implements ProvisionListener {
        private LifecycleManager manager;
        private List<LifecycleListener> pendingLifecycleListeners = new ArrayList<>();
        
        @Inject
        public static void initialize(LifecycleManager manager, LifecycleListenerProvisionListener provisionListener) {
            provisionListener.manager = manager;
            for (LifecycleListener l : provisionListener.pendingLifecycleListeners) {
                manager.addListener(l);
            }
            provisionListener.pendingLifecycleListeners.clear();
        }
                
        @Override
        public String toString() {
            return "LifecycleListenerProvisionListener[]";
        }

        @Override
        public <T> void onProvision(ProvisionInvocation<T> provision) {
            final T injectee = provision.provision();
            if (injectee != null && injectee instanceof LifecycleListener) {
                if (manager == null) {
                    pendingLifecycleListeners.add((LifecycleListener) injectee);
                } else {
                    manager.addListener((LifecycleListener) injectee);
                }
            }
        }
    }

    @Override
    public boolean equals(Object obj) {
        return getClass()==obj.getClass();
    }

    @Override
    public int hashCode() {
        return getClass().hashCode();
    }

    @Override
    public String toString() {
        return "LifecycleListenerModule[]";
    }
}
