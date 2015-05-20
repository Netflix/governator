package com.netflix.governator;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;

import javax.inject.Singleton;

import com.google.inject.Inject;
import com.google.inject.Injector;

/**
 * Utility class for managing lifecycle within a Guice {@link Injector}.  LifecycleManager
 * is very loosly couples with Guice and simply provide a machanism to register 
 * {@link LifecycleListener}'s that can be invoked during shutdown.  Shutdown may be triggered
 * either from outside the injector using {@link LifecycleManager.shutdown(injector)} or
 * internally from within the injector by injecting LifecycleManager and calling shutdown.
 * 
 * LifecycleListener handlers should only be registered by frameworks and not used directly
 * by application code.  Application code should instead add the LifecycleModule and annotate
 * any shutdown methods with @PreDestroy.  LifecycleModule registers it's own LifecycleListener
 * to invoke all the @PreDestroy methods at shutdown.
 * 
 * <b>Invoking shutdown from outside the injector</b>
 * <pre>
 * {@code 
 *    Injector injector = Guice.createInjector();
 *    // ...
 *    LifecycleManager.shutdown(injector);
 * }
 * 
 * </pre>
 * 
 * <b>Blocking on the injector terminating</b>
 * <pre>
 * {@code 
 *    LifecycleManager.awaitTermination(Guice.createInjector());
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
 *    LifecycleManager.onShutdown(
 *      Guice.createInjector(),
 *      new LifecycleListener() {
 *          public void onShutdown() {
 *              // Do your shutdown handling here
 *          }
 *      });
 * }
 * </pre>
 * @author elandau
 */
@Singleton
public class LifecycleManager {
    private final CopyOnWriteArraySet<LifecycleListener> listeners = new CopyOnWriteArraySet<>();
    private final CountDownLatch latch = new CountDownLatch(1);
    
    @Inject(optional=true)
    public void addListeners(Set<LifecycleListener> listeners) {
        this.listeners.addAll(listeners);
    }
    
    public void addListener(LifecycleListener listener) {
        listeners.add(listener);
    }
    
    public void shutdown() {
        if (listeners != null) {
            for (LifecycleListener hook : listeners) {
                hook.onShutdown();
            }
        }
        latch.countDown();
    }
    
    public void awaitTermination() throws InterruptedException {
        latch.await();
    }
}
