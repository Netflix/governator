package com.netflix.governator.lifecycle;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nullable;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;
import com.netflix.governator.LifecycleManager;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.guice.LifecycleInjectorMode;
import com.netflix.governator.spi.LifecycleListener;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class LifeCycleFeaturesOnLegacyBuilderTest {
    static class LifecycleSubject {
        private Logger logger = LoggerFactory.getLogger(LifecycleSubject.class);
        private String name;
        private volatile boolean postConstructed = false;
        private volatile boolean preDestroyed = false;

        private static AtomicInteger instanceCounter = new AtomicInteger(0);

        public LifecycleSubject(String name) {
            this.name = name;
            instanceCounter.incrementAndGet();
            logger.info("created instance " + this);
        }

        @PostConstruct
        public void init() {
            logger.info("@PostConstruct called " + this);
            this.postConstructed = true;
        }

        @PreDestroy
        public void destroy() {
            logger.info("@PreDestroy called " + this);
            this.preDestroyed = true;
        }

        public boolean isPostConstructed() {
            return postConstructed;
        }

        public boolean isPreDestroyed() {
            return preDestroyed;
        }

        public String getName() {
            return name;
        }

        public static int getInstanceCount() {
            return instanceCounter.get();
        }

        public String toString() {
            return "LifecycleSubject@" + System.identityHashCode(this) + '[' + name + ']';
        }
    }
    
    static class UsesNullable {
        private AtomicBoolean preDestroyed = new AtomicBoolean(false);
        private LifecycleSubject subject;
        @Inject
        public void UsesNullable(@Nullable @Named("missing") LifecycleSubject subject) {
            this.subject = subject;
        }
        
        @PreDestroy
        public void destroy() {
            this.preDestroyed.set(true);
        }
   }
    
    static interface LifecycleInterface {
        @PostConstruct
        public default void init() {
            System.out.println("init() called");
        }
        @PreDestroy
        public default void destroy() {
            System.out.println("destroy() called");
        }

    }

    @Mock
    private TestListener listener;

    private Injector injector;

    private LocalScope localScope;

    private static class TestListener implements LifecycleListener {
        @PostConstruct
        public void init() {
        }

        @PreDestroy
        public void shutdown() {
        }

        public void onStopped(Throwable error) {
        }

        public void onStarted() {
        }
    }

    public com.netflix.governator.lifecycle.LifecycleManager init(LifecycleInjectorBuilder builder)  throws Exception {
        LifecycleSubject.instanceCounter.set(0);
        localScope = new LocalScope();
        listener = Mockito.mock(TestListener.class);
        LifecycleInjector lifecycleInjector = builder
                .withAdditionalModules(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(TestListener.class).toInstance(listener);
                        bindScope(LocalScoped.class, localScope);
                        bind(UsesNullable.class);
                        bind(Key.get(LifecycleSubject.class, Names.named("missing"))).toProvider(Providers.of((LifecycleSubject)null));
                    }

                    @Provides
                    @LocalScoped
                    @Named("thing1")
                    public LifecycleSubject thing1() {
                        return new LifecycleSubject("thing1");
                    }

                    @Provides
                    @Singleton
                    @Named("thing2")
                    public LifecycleSubject thing2() {
                        return new LifecycleSubject("thing2");
                    }
                })
                .requiringExplicitBindings()
                .build();
        injector = lifecycleInjector.createInjector();
        com.netflix.governator.lifecycle.LifecycleManager lifecycleManager = lifecycleInjector.getLifecycleManager();
        lifecycleManager.start();
        return lifecycleManager;
    }

    @UseDataProvider("builders")
    @Test
    public void testPostActionsAndLifecycleListenersInvoked(LifecycleInjectorBuilder builder) throws Exception {
        try (com.netflix.governator.lifecycle.LifecycleManager lm = init(builder)) {
            assertNotNull(injector);
            assertNotNull(injector.getInstance(LifecycleManager.class));
    
            Mockito.verify(listener, Mockito.times(1)).onStarted();
            Mockito.verify(listener, Mockito.times(1)).init();
            Mockito.verify(listener, Mockito.times(0)).onStopped(Mockito.any(Throwable.class));
            Mockito.verify(listener, Mockito.times(0)).shutdown();
        }
        Mockito.verify(listener, Mockito.times(1)).onStopped(Mockito.any(Throwable.class));
        Mockito.verify(listener, Mockito.times(1)).onStopped(Mockito.any(Throwable.class));
    }

    @UseDataProvider("builders")
    @Test
    public void testScopeManagement(LifecycleInjectorBuilder builder) throws Exception {
        LifecycleSubject thing2 = null;
        try (com.netflix.governator.lifecycle.LifecycleManager lm = init(builder)) {
            thing2 = injector.getInstance(Key.get(LifecycleSubject.class, Names.named("thing2")));
            localScope.enter();
            injector.getInstance(Key.get(LifecycleSubject.class, Names.named("thing1")));
            LifecycleSubject thing1 = injector.getInstance(Key.get(LifecycleSubject.class, Names.named("thing1")));
            Assert.assertTrue(thing1.isPostConstructed());
            Assert.assertFalse(thing1.isPreDestroyed());
            Assert.assertTrue(thing2.isPostConstructed());
            Assert.assertFalse(thing2.isPreDestroyed());
            Assert.assertEquals(2, LifecycleSubject.getInstanceCount()); // thing1 and
                                                                         // thing2
            Assert.assertFalse(thing2.isPreDestroyed());
            localScope.exit();

            System.gc();
            Thread.sleep(500);
            Assert.assertTrue(thing1.isPreDestroyed());
        }
        System.gc();
        Thread.sleep(500);
        Assert.assertTrue(thing2.isPreDestroyed());
    }
    
    @UseDataProvider("builders")
    @Test
    public void testSingletonScopeManagement(LifecycleInjectorBuilder builder) throws Exception {
        LifecycleSubject thing2 = null;
        try (com.netflix.governator.lifecycle.LifecycleManager lm = init(builder)) {
            thing2 = injector.getInstance(Key.get(LifecycleSubject.class, Names.named("thing2")));
            Assert.assertTrue(thing2.isPostConstructed());
            Assert.assertFalse(thing2.isPreDestroyed());
            
            injector.getInstance(Key.get(LifecycleSubject.class, Names.named("thing2")));
            injector.getInstance(Key.get(LifecycleSubject.class, Names.named("thing2")));
            injector.getInstance(Key.get(LifecycleSubject.class, Names.named("thing2")));
            
            Assert.assertEquals(1, LifecycleSubject.getInstanceCount()); 
    
        }
        System.gc();
        Thread.sleep(500);
        Assert.assertTrue(thing2.isPreDestroyed());

    }   
    
    @UseDataProvider("builders")
    @Test
    public void testNullableInjection(LifecycleInjectorBuilder builder) throws Exception {
        AtomicBoolean nullableConsumerDestroyed = null;
        try (com.netflix.governator.lifecycle.LifecycleManager lm = init(builder)) {
            UsesNullable nullableConsumer = injector.getInstance(UsesNullable.class);
            nullableConsumerDestroyed = nullableConsumer.preDestroyed;
            Assert.assertNull(nullableConsumer.subject);   
        }
        System.gc();
        Thread.sleep(500);
        Assert.assertTrue(nullableConsumerDestroyed.get());
    }       
    
    @Test
    public void testLifecycleMethods() throws Exception {
        LifecycleInterface instance = new LifecycleInterface() {};
        Class<?> defaultMethodsClass = instance.getClass();
        LifecycleMethods methods = new LifecycleMethods(defaultMethodsClass);
        methods.methodInvoke(defaultMethodsClass.getMethod("init", (Class[])null), instance);
        methods.methodInvoke(defaultMethodsClass.getMethod("destroy", (Class[])null), instance);
    }
    
    
    @DataProvider
    public static Object[][] builders()
    {
        return new Object[][]
        {
            new Object[] { "simulatedChildInjector", LifecycleInjector.builder().withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS) },
            new Object[] { "childInjector", LifecycleInjector.builder() },
            new Object[] { "simulatedChildInjectorExplicitBindings", LifecycleInjector.builder().withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS).requiringExplicitBindings() },
            new Object[] { "childInjectorExplicitBindings", LifecycleInjector.builder().requiringExplicitBindings() }
            
        };
    }
}
