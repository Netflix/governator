package com.netflix.governator;

import java.lang.reflect.Method;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.multibindings.Multibinder;
import com.google.inject.spi.InjectionListener;
import com.google.inject.spi.TypeEncounter;
import com.google.inject.spi.TypeListener;

/**
 * Adding {@link LifecycleModule} to the injector will install a TypeListener that
 * provides support for @PostConstruct and @PreDestroy methods.
 * 
 * Triggering the calls to @PreDestroy methods is done by registering a ShutdownHook.
 * It is the responsibility of the code creating the Injector to invoke the shutdown
 * hooks by get an instance of ShutdownInvoker.
 * 
 * For example,
 * <pre>
 * public class MyService {
 *    @PostConstruct
 *    public void init() {
 *    }
 *    
 *    @PreDestroy
 *    public void shutdown() {
 *    }
 * }
 * 
 * </pre>
 * @author elandau
 *
 */
public class LifecycleModule extends AbstractModule {
    private static final Logger LOG = LoggerFactory.getLogger(LifecycleModule.class);

    public static class LifecycleTypeListener implements TypeListener, LifecycleListener {
        final List<Runnable> actions = new LinkedList<Runnable>();
        final AtomicBoolean isShutdown = new AtomicBoolean();
        
        @Override
        public <I> void hear(TypeLiteral<I> typeLiteral, TypeEncounter<I> encounter) {
            Class<?> clazz = typeLiteral.getRawType();
            
            // Look for @PostConstruct and @PreDestroy methods
            final Set<Method> postConstruct = new LinkedHashSet<>();
            final Set<Method> preDestroy = new LinkedHashSet<>();
            
            discoverMethods(clazz, postConstruct, preDestroy);
            
            if (!postConstruct.isEmpty() || !preDestroy.isEmpty()) {
                encounter.register(new InjectionListener<I>() {
                    @Override
                    public void afterInjection(final I injectee) {
                        for (Method m : postConstruct) {
                            try {
                                m.invoke(injectee);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                        }
                        
                        if (!preDestroy.isEmpty()) {
                            synchronized (LifecycleTypeListener.this) {
                                for (final Method m : preDestroy) {
                                    if (isShutdown.get() == false) {
                                        actions.add(new Runnable() {
                                            @Override
                                            public void run() {
                                                try {
                                                    m.invoke(injectee);
                                                } catch (Exception e) {
                                                    LOG.error("Failed to call @PreDestroy method {} on {}", new Object[]{m.getName(), injectee.getClass().getName()}, e);
                                                }
                                            }
                                        });
                                    }
                                    else {
                                        LOG.info("Already shutting down.  Shutdown method {} on {} will not be invoked", new Object[]{m.getName(), injectee.getClass().getName()});
                                    }
                                }
                            }
                        }
                    }
                });
            }
        }
        
        /**
         * Recursively discovery all lifecycle methods
         * 
         * @param type
         * @param postConstruct
         * @param preDestroy
         */
        private static void discoverMethods(Class<?> type, Set<Method> postConstruct, Set<Method> preDestroy) {
            if (type == null) {
                return;
            }
            
            for (Method method : type.getDeclaredMethods()) {
                if (method.isAnnotationPresent(PostConstruct.class)) {
                    method.setAccessible(true);
                    postConstruct.add(method);
                }
                else if (method.isAnnotationPresent(PreDestroy.class)) {
                    method.setAccessible(true);
                    preDestroy.add(method);
                }
            }
            
            discoverMethods(type.getSuperclass(), postConstruct, preDestroy);
            for (Class<?> i : type.getInterfaces()) {
                discoverMethods(i, postConstruct, preDestroy);
            }
        }
        
        /**
         * Invoke all shutdown actions
         */
        @Override
        public synchronized void onShutdown() {
            isShutdown.set(true);
            for (Runnable action : actions) {
                action.run();
            }
        }
    }
    
    @Override
    protected void configure() {
        LifecycleTypeListener listener = new LifecycleTypeListener();
        this.bindListener(Matchers.any(), listener);
        Multibinder.newSetBinder(binder(), LifecycleListener.class).addBinding().toInstance(listener);
   }
}