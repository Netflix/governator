package com.netflix.governator.lifecycle;

import static org.junit.Assert.assertNotNull;

import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.netflix.governator.LifecycleManager;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.spi.LifecycleListener;

@RunWith(MockitoJUnitRunner.class)
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

    private com.netflix.governator.lifecycle.LifecycleManager legacyLifecycleManager;

    @Mock
    private TestListener listener;

    private LifecycleManager lifecycleManager;

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

    @Before
    public void init() throws Exception {
        LifecycleSubject.instanceCounter.set(0);
        localScope = new LocalScope();
        LifecycleInjector lifecycleInjector = LifecycleInjector.builder()
                // .withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS)
                .withAdditionalModules(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(TestListener.class).toInstance(listener);
                        bindScope(LocalScoped.class, localScope);
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
                }).build();
        injector = lifecycleInjector.createInjector();
        legacyLifecycleManager = lifecycleInjector.getLifecycleManager();

        lifecycleManager = injector.getInstance(LifecycleManager.class);
        legacyLifecycleManager.start();
    }

    @Test
    public void testPostActionsAndLifecycleListenersInvoked() {
        assertNotNull(injector);
        assertNotNull(lifecycleManager);

        Mockito.verify(listener, Mockito.times(1)).onStarted();
        Mockito.verify(listener, Mockito.times(1)).init();
        Mockito.verify(listener, Mockito.times(0)).onStopped(Mockito.any(Throwable.class));
        Mockito.verify(listener, Mockito.times(0)).shutdown();
        legacyLifecycleManager.close();
        Mockito.verify(listener, Mockito.times(1)).onStopped(Mockito.any(Throwable.class));
        Mockito.verify(listener, Mockito.times(1)).onStopped(Mockito.any(Throwable.class));
    }

    @Test
    public void testScopeManagement() throws Exception {
        LifecycleSubject thing2 = injector.getInstance(Key.get(LifecycleSubject.class, Names.named("thing2")));
        localScope.enter();
        injector.getInstance(Key.get(LifecycleSubject.class, Names.named("thing1")));
        LifecycleSubject thing1 = injector.getInstance(Key.get(LifecycleSubject.class, Names.named("thing1")));
        Assert.assertTrue(thing1.isPostConstructed());
        Assert.assertFalse(thing1.isPreDestroyed());
        Assert.assertTrue(thing2.isPostConstructed());
        Assert.assertFalse(thing2.isPreDestroyed());
        Assert.assertEquals(2, LifecycleSubject.getInstanceCount()); // thing1 and
                                                                     // thing2
        System.gc();
        Thread.sleep(500);
        Assert.assertFalse(thing2.isPreDestroyed());

        localScope.exit();
        System.gc();
        Thread.sleep(500);
        Assert.assertTrue(thing1.isPreDestroyed());
        legacyLifecycleManager.close();
        Assert.assertTrue(thing2.isPreDestroyed());

    }
}
