package com.netflix.governator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.junit.Test;

import com.google.inject.Injector;
import com.netflix.governator.Governator;
import com.netflix.governator.LifecycleListener;

public class LifecycleModuleTest {
    @Test
    public void confirmLifecycleListenerEventsForSuccessfulStart() {
        final List<String> events = new ArrayList<>();
        
        new Governator()
            .run(new LifecycleListener() {
                @Inject
                public void initialize(Injector injector) {
                    events.add("injected");
                }
                
                @PostConstruct
                public void initialized() {
                    events.add("initialized");
                }
                
                @PreDestroy
                public void destroyed() {
                    events.add("destroyed");
                }
                
                @Override
                public void onStarted() {
                    events.add("started");
                }

                @Override
                public void onStopped() {
                    events.add("stopped");
                }

                @Override
                public void onStartFailed(Throwable t) {
                    events.add("failed");
                }

                @Override
                public void onFinished() {
                    events.add("finished");
                }
                
                @Override
                public String toString() {
                    return "confirmLifecycleListenerEventsForSuccessfulStart";
                }
            })
            .shutdown();
        
        assertThat(events,  equalTo(Arrays.asList("injected", "initialized", "started", "destroyed", "stopped", "finished")));
    }
    
    @Test(expected=RuntimeException.class)
    public void confirmLifecycleListenerEventsForFailedStart() {
        final List<String> events = new ArrayList<>();
        
        try {
            new Governator()
                .run(new LifecycleListener() {
                    @Inject
                    public void initialize(Injector injector) {
                        events.add("injected");
                        throw new RuntimeException("Failed");
                    }
                    
                    @PostConstruct
                    public void initialized() {
                        events.add("initialized");
                    }
                    
                    @PreDestroy
                    public void destroyed() {
                        events.add("destroyed");
                    }
                    
                    @Override
                    public void onStarted() {
                        events.add("started");
                    }
    
                    @Override
                    public void onStopped() {
                        events.add("stopped");
                    }
    
                    @Override
                    public void onStartFailed(Throwable t) {
                        events.add("failed");
                    }
    
                    @Override
                    public void onFinished() {
                        events.add("finished");
                    }
                    
                    @Override
                    public String toString() {
                        return "confirmLifecycleListenerEventsForSuccessfulStart";
                    }
                })
                .shutdown();
        }
        finally {
            assertThat(events,  equalTo(Arrays.asList("injected", "initialized", "destroyed", "failed", "finished")));
        }
    }
    
    @Test(expected=AssertionError.class)
    public void assertionExampleInListener() {
        new Governator()
            .run(new LifecycleListener() {
                @Override
                public void onStarted() {
                }
    
                @Override
                public void onStopped() {
                }
    
                @Override
                public void onStartFailed(Throwable t) {
                }
    
                @Override
                public void onFinished() {
                    assertThat(false, equalTo(true));
                }
            })
            .shutdown();
    }
}
