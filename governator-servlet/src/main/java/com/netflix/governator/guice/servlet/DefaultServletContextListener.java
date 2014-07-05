package com.netflix.governator.guice.servlet;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import com.netflix.governator.guice.lazy.LazySingleton;

/**
 * Default no-op implementation of a ServletContextListener within the context of
 * Guice.
 * 
 * @author elandau
 *
 */
@LazySingleton
public final class DefaultServletContextListener implements ServletContextListener {
    /**
     * Called after the injector and all eager singletons have been created and
     * LifecycleManager.start() was called.  
     */
    @Override
    public void contextInitialized(final ServletContextEvent event) {
    }

    /**
     * Called before LifecycleManager.shutdown() is called.
     */
    @Override
    public void contextDestroyed(final ServletContextEvent event) {
    }
}
