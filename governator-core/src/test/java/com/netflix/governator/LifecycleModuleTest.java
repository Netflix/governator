package com.netflix.governator;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.inject.Injector;
import com.netflix.governator.spi.LifecycleListener;

public class LifecycleModuleTest {
    
    private static final class AssertionErrorListener extends TrackingLifecycleListener {
        public AssertionErrorListener(String name) {
            super(name);
        }
        @PreDestroy
        @Override
        public void destroyed() {
            super.destroyed();
            assertThat(false, equalTo(true));
        }
    }

    private static enum Events {
        Injected,
        Initialized,
        Destroyed,
        Started,
        Stopped,
        Error
    }
    
    @Rule
    public final TestName name = new TestName();
    
    static class TrackingLifecycleListener implements LifecycleListener {
        final List<Events> events = new ArrayList<>();
        private String name;
        
        public TrackingLifecycleListener(String name) {
            this.name = name;
        }
        
        @Inject
        public void initialize(Injector injector) {
            events.add(Events.Injected);
        }
        
        @PostConstruct
        public void initialized() {
            events.add(Events.Initialized);
        }
        
        @PreDestroy
        public void destroyed() {
            events.add(Events.Destroyed);
        }
        
        @Override
        public void onStarted() {
            events.add(Events.Started);
        }

        @Override
        public void onStopped(Throwable t) {
            events.add(Events.Stopped);
            if (t != null) {
                events.add(Events.Error);
            }
        }

        @Override
        public String toString() {
            return "TrackingLifecycleListener[" + name + "]";
        }
    }
    
    @Test
    public void confirmLifecycleListenerEventsForSuccessfulStart() {
        final TrackingLifecycleListener listener = new TrackingLifecycleListener(name.getMethodName());
        
        TestSupport.inject(listener).close();
        
        assertThat(listener.events, equalTo(Arrays.asList(Events.Injected, Events.Initialized, Events.Started, Events.Stopped, Events.Destroyed)));
    }
    
    @Test(expected=RuntimeException.class)
    public void confirmLifecycleListenerEventsForFailedStart() {
        final TrackingLifecycleListener listener = new TrackingLifecycleListener(name.getMethodName()) {
            @Override
            @Inject
            public void initialize(Injector injector) {
                super.initialize(injector);
                throw new RuntimeException("Failed");
            } 
        };
        
        try (LifecycleInjector injector = TestSupport.inject(listener)){
        }
        finally {
            assertThat(listener.events, equalTo(Arrays.asList(Events.Injected)));
        }
    }
    
    @Ignore // deprecated governator.run() does not invoke lifecycle methods 
    @Test(expected=AssertionError.class)
<<<<<<< 31d39215f31ed941452ddfb04d9ea74fd36f7815
    public void assertionExceptionInListener() {
        TrackingLifecycleListener listener = new TrackingLifecycleListener() {
            @Override
            public void onStopped(Throwable t) {
                super.onStopped(t);
                assertThat(t, nullValue());
                assertThat(false, equalTo(true));
            }
        };
        try (LifecycleInjector injector = TestSupport.inject(listener)){
        }
=======
    public void assertionErrorInListener() {
        AssertionErrorListener listener = new AssertionErrorListener(name.getMethodName());
        LifecycleInjector lifecycleInjector = new Governator().run(listener);
        lifecycleInjector.getInstance(AssertionErrorListener.class);
        lifecycleInjector.shutdown();
>>>>>>> ordering for lifecycle listeners, fix equality in SafeLifecycleListener
    }

}
