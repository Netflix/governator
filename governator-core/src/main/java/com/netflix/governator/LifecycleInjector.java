package com.netflix.governator;

import com.google.inject.Injector;

/**
 * Wrapper for Guice's Injector with added shutdown methods.  A LifecycleInjector may be created
 * using the utility methods of {@link Governator} which mirror the methods of {@link Guice}
 * but provide shutdown functionality.
 * 
 * <b>Invoking shutdown from outside the injector</b>
 * <pre>
 * {@code 
 *    LifecycleInjector injector = Governator.createInjector();
 *    // ...
 *    injector.shutdown();
 * }
 * 
 * </pre>
 * 
 * <b>Blocking on the injector terminating</b>
 * <pre>
 * {@code 
 *    LifecycleInjector injector = Governator.createInjector();
 *    // ...
 *    injector.awaitTermination();
 * }
 * </pre>
 * 
 * <b>Triggering shutdown from a DI'd class
 * <pre>
 * {@code 
 *    @Singleton
 *    public class SomeShutdownService {
 *        @Inject
 *        SomeShutdownService(LifecycleManager lifecycleManager) {
 *            this.lifecycleManager = lifecycleManager;
 *        }
 *      
 *        void someMethodInvokedForShutdown() {
 *            this.lifecycleManager.shutdown();
 *        }
 *    }
 * }
 * </pre>
 * 
 * <b>Triggering an external event from shutdown without blocking</b>
 * <pre>
 * {@code 
 *    LifecycleInjector injector = Governator.createInjector();
 *    injector.addListener(new LifecycleListener() {
 *        public void onShutdown() {
 *            // Do your shutdown handling here
 *        }
 *    });
 * }
 * </pre>
 * @author elandau
 */
public class LifecycleInjector extends DelegatingInjector {
    private LifecycleManager manager;
    
    public LifecycleInjector(Injector injector, LifecycleManager manager) {
        super(injector);
        this.manager  = manager;
    }
    
    /**
     * Block until LifecycleManager terminates
     * 
     * @param injector
     * @throws InterruptedException
     */
    public void awaitTermination() throws InterruptedException {
        manager.awaitTermination();
    }

    /**
     * Shutdown LifecycleManager on this Injector which will invoke all registered
     * {@link LifecycleListener}s and unblock awaitTermination. 
     * 
     * @param injector
     */
    public void shutdown() {
        manager.shutdown();
    }
    
    /**
     * Register a single shutdown listener for async notification of the LifecycleManager
     * terminating. 
     * 
     * @param injector
     * @param listener
     */
    public void addListener(LifecycleListener listener) {
        manager.addListener(listener);
    }
}
