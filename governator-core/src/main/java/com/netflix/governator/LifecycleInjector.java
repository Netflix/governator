package com.netflix.governator;

import com.google.inject.Injector;
import com.netflix.governator.spi.LifecycleListener;

/**
 * Wrapper for Guice's Injector with added shutdown methods.  
 * 
 * <b>Invoking shutdown from outside the injector</b>
 * <pre>
 * <code>
 *    LifecycleInjector injector = new Governator().run();
 *    // ...
 *    injector.shutdown();
 * </code>
 * </pre>
 * 
 * <b>Blocking on the injector terminating</b>
 * <pre>
 * <code>
 *    LifecycleInjector injector = new Governator().run(;
 *    // ...
 *    injector.awaitTermination();
 * </code>
 * </pre>
 * 
 * <b>Triggering shutdown from a DI'd class</b>
 * <pre>
 * <code>
 *    {@literal @}Singleton
 *    public class SomeShutdownService {
 *        {@literal @}Inject
 *        SomeShutdownService(LifecycleManager lifecycleManager) {
 *            this.lifecycleManager = lifecycleManager;
 *        }
 *      
 *        void someMethodInvokedForShutdown() {
 *            this.lifecycleManager.shutdown();
 *        }
 *    }
 * }
 * </code>
 * </pre>
 * 
 * <b>Triggering an external event from shutdown without blocking</b>
 * <pre>
 * <code>
 *    LifecycleInjector injector = new Governator().run(;
 *    injector.addListener(new LifecycleListener() {
 *        public void onShutdown() {
 *            // Do your shutdown handling here
 *        }
 *    });
 * }
 * </code>
 * </pre>
 */
final public class LifecycleInjector extends DelegatingInjector {
    private final LifecycleManager manager;
    private final LifecycleShutdownSignal signal;
    
    public static LifecycleInjector createFailedInjector(LifecycleManager manager) {
        return new LifecycleInjector(null, manager);
    }
    
    public static LifecycleInjector wrapInjector(Injector injector, LifecycleManager manager) {
        return new LifecycleInjector(injector, manager);
    }
    
    private LifecycleInjector(Injector injector, LifecycleManager manager) {
        super(injector);
        this.manager  = manager;
        if (injector != null) {
            this.signal = injector.getInstance(LifecycleShutdownSignal.class);
        }
        else {
            this.signal = new DefaultLifecycleShutdownSignal(manager);
        }
    }
    
    /**
     * Block until LifecycleManager terminates
     * @throws InterruptedException
     */
    public void awaitTermination() throws InterruptedException {
        signal.await();
    }

    /**
     * Shutdown LifecycleManager on this Injector which will invoke all registered
     * {@link LifecycleListener}s and unblock awaitTermination. 
     */
    public void shutdown() {
        signal.signal();
    }
    
    /**
     * Register a single shutdown listener for async notification of the LifecycleManager
     * terminating. 
     * @param listener
     */
    public void addListener(LifecycleListener listener) {
        manager.addListener(listener);
    }
}
