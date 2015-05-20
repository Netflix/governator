package com.netflix.governator.guice;

import com.google.inject.Injector;
import com.netflix.governator.LifecycleListener;
import com.netflix.governator.LifecycleManager;

/**
 * Utility class for managing LifecycleManager within a Guice {@link Injector}.  LifecycleManager
 * is very loosely coupled with Guice and simply provides a mechanism to register 
 * {@link LifecycleListener}s that can be invoked during shutdown.  Shutdown may be triggered
 * either from outside the injector using {@link InjectorLifecycle.shutdown(injector)} or
 * from within a class managed by the injector by injecting LifecycleManager and calling shutdown.
 * 
 * LifecycleListener handlers should only be registered by frameworks and not used directly
 * by application code.  Application code should instead add the LifecycleModule and annotate
 * any shutdown methods with @PreDestroy.  LifecycleModule registers its own LifecycleListener
 * to invoke all the @PreDestroy methods at shutdown.
 * 
 * <b>Invoking shutdown from outside the injector</b>
 * <pre>
 * {@code 
 *    Injector injector = Guice.createInjector();
 *    // ...
 *    InjectorLifecycle.shutdown(injector);
 * }
 * 
 * </pre>
 * 
 * <b>Blocking on the injector terminating</b>
 * <pre>
 * {@code 
 *    Injector injector = Guice.createInjector();
 *    // ...
 *    InjectorLifecycle.awaitTermination(injector);
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
 *    Injector injector = Guice.createInjector();
 *    InjectorLifecycle.onShutdown(
 *      injector,
 *      new LifecycleListener() {
 *          public void onShutdown() {
 *              // Do your shutdown handling here
 *          }
 *      });
 * }
 * </pre>
 * @author elandau
 */
public class InjectorLifecycle {
    /**
     * Block until LifecycleManager terminates
     * 
     * @param injector
     * @throws InterruptedException
     */
    public static void awaitTermination(Injector injector) throws InterruptedException {
        injector.getInstance(LifecycleManager.class).awaitTermination();
    }

    /**
     * Shutdown LifecycleManager on this Injector which will invoke all registered
     * {@link LifecycleListener}s and unblock awaitTermination. 
     * 
     * @param injector
     */
    public static void shutdown(Injector injector) {
        injector.getInstance(LifecycleManager.class).shutdown();
    }
    
    /**
     * Register a single shutdown listener for async notification of the LifecycleManager
     * terminating. 
     * 
     * @param injector
     * @param listener
     */
    public static void onShutdown(Injector injector, LifecycleListener listener) {
        injector.getInstance(LifecycleManager.class).addListener(listener);
    }
}
