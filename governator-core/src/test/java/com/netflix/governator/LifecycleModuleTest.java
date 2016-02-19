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

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.inject.Injector;
import com.netflix.governator.spi.LifecycleListener;

public class LifecycleModuleTest {
    
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
    
    class TrackingLifecycleListener implements LifecycleListener {
        final List<Events> events = new ArrayList<>();
        
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
            return "TrackingLifecycleListener[" + name.getMethodName() + "]";
        }
    }
    
    @Test
    public void confirmLifecycleListenerEventsForSuccessfulStart() {
        final TrackingLifecycleListener listener = new TrackingLifecycleListener();
        
        new Governator()
            .run(listener)
            .shutdown();
        
        assertThat(listener.events, equalTo(Arrays.asList(Events.Injected, Events.Initialized, Events.Started, Events.Destroyed, Events.Stopped)));
    }
    
    @Test(expected=RuntimeException.class)
    public void confirmLifecycleListenerEventsForFailedStart() {
        final TrackingLifecycleListener listener = new TrackingLifecycleListener() {
            @Override
            @Inject
            public void initialize(Injector injector) {
                super.initialize(injector);
                throw new RuntimeException("Failed");
            } 
        };
        
        try {
            new Governator()
                .run(listener)
                .shutdown();
        }
        finally {
            assertThat(listener.events, equalTo(Arrays.asList(Events.Injected, Events.Initialized, Events.Stopped, Events.Error, Events.Destroyed)));
        }
    }
    
    @Test(expected=AssertionError.class)
    public void assertionExampleInListener() {
        new Governator()
            .run(new TrackingLifecycleListener() {
                @Override
                public void onStopped(Throwable t) {
                    super.onStopped(t);
                    assertThat(t, nullValue());
                    assertThat(false, equalTo(true));
                }
            })
            .shutdown();
    }
}
