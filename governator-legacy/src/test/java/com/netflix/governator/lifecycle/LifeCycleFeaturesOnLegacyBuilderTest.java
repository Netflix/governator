package com.netflix.governator.lifecycle;

import static org.junit.Assert.assertNotNull;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.netflix.governator.LifecycleManager;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.spi.LifecycleListener;

@RunWith(MockitoJUnitRunner.class)
public class LifeCycleFeaturesOnLegacyBuilderTest {
    
    private com.netflix.governator.lifecycle.LifecycleManager legacyLifecycleManager;

    @Mock
    private TestListener listener;

    private LifecycleManager lifecycleManager;

    private Injector injector;
    
    private static class TestListener implements LifecycleListener {
        @PostConstruct public void init() {}
        @PreDestroy public void shutdown() {}
        public void onStopped(Throwable error) {}
        public void onStarted() {}
    }

    @Before
    public void init() throws Exception {
        LifecycleInjector lifecycleInjector = LifecycleInjector.builder()
                .withAdditionalModules(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(TestListener.class).toInstance(listener);
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
}
