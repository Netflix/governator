package com.netflix.governator;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;

import com.google.inject.CreationException;
import com.google.inject.Injector;
import com.netflix.governator.spi.LifecycleListener;

public class LifecycleModuleTest {

    static class TestRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TestRuntimeException() {
            super();
        }

        public TestRuntimeException(String message) {
            super(message);
        }

    }

    private static enum Events {
        Injected, Initialized, Destroyed, Started, Stopped, Error
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
        public void injected(Injector injector) {
            events.add(Events.Injected);
        }

        @PostConstruct
        public void initialized() {
            events.add(Events.Initialized);
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

        @PreDestroy
        public void destroyed() {
            events.add(Events.Destroyed);
        }

        @Override
        public String toString() {
            return "TrackingLifecycleListener@" + name;
        }
    }

    @Test
    public void confirmLifecycleListenerEventsForSuccessfulStart() {
        final TrackingLifecycleListener listener = new TrackingLifecycleListener(name.getMethodName());

        TestSupport.inject(listener).close();

        assertThat(listener.events, equalTo(
                Arrays.asList(Events.Injected, Events.Initialized, Events.Started, Events.Stopped, Events.Destroyed)));
    }

    @Test
    public void confirmLifecycleListenerEventsForRTExceptionInjected() {
        final TrackingLifecycleListener listener = new TrackingLifecycleListener(name.getMethodName()) {
            @Override
            @Inject
            public void injected(Injector injector) {
                super.injected(injector);
                throw new TestRuntimeException("injected failed");
            }
        };

        try (LifecycleInjector injector = TestSupport.inject(listener)) {
            fail("expected exception injecting instance");
        } catch (CreationException e) {
            // expected
        } catch (Exception e) {
            fail("expected CreationException injecting instance but got " + e);
        } finally {
            assertThat(listener.events, equalTo(Arrays.asList(Events.Injected)));
        }
    }
    
    @Test
    public void confirmLifecycleListenerEventsForRTExceptionPostConstruct() {
        final TrackingLifecycleListener listener = new TrackingLifecycleListener(name.getMethodName()) {
            @Override
            @PostConstruct
            public void initialized() {
                super.initialized();
                throw new TestRuntimeException("postconstruct rt exception");
            }
        };

        try (LifecycleInjector injector = TestSupport.inject(listener)) {
            fail("expected rt exception starting injector");
        } catch (CreationException e) {
            // expected
        } catch (Exception e) {
            fail("expected CreationException starting injector but got " + e);
        } finally {
            assertThat(listener.events, equalTo(Arrays.asList(Events.Injected, Events.Initialized, Events.Stopped, Events.Error)));
        }
    }

    @Test
    public void confirmLifecycleListenerEventsForRTExceptionOnStarted() {
        final TrackingLifecycleListener listener = new TrackingLifecycleListener(name.getMethodName()) {
            @Override
            public void onStarted() {
                super.onStarted();
                throw new TestRuntimeException("onStarted rt exception");
            }
        };

        try (LifecycleInjector injector = TestSupport.inject(listener)) {
            fail("expected rt exception starting injector");
        } catch (TestRuntimeException e) {
            // expected
        } catch (Exception e) {
            fail("expected TestRuntimeException starting injector but got " + e);
        } finally {
            assertThat(listener.events, equalTo(Arrays.asList(Events.Injected, Events.Initialized, Events.Started, Events.Stopped, Events.Error, Events.Destroyed)));
        }
    }

    @Test
    public void confirmLifecycleListenerEventsForRTExceptionOnStopped() {
        final TrackingLifecycleListener listener = new TrackingLifecycleListener(name.getMethodName()) {
            @Override
            public void onStopped(Throwable t) {
                super.onStopped(t);
                throw new TestRuntimeException("onStopped rt exception");
            }
        };

        try (LifecycleInjector injector = TestSupport.inject(listener)) {
        } finally {
            assertThat(listener.events, equalTo(Arrays.asList(Events.Injected, Events.Initialized, Events.Started, Events.Stopped, Events.Destroyed)));
        }
    }

    @Test
    public void confirmLifecycleListenerEventsForRTExceptionPreDestroy() {
        final TrackingLifecycleListener listener = new TrackingLifecycleListener(name.getMethodName()) {
            @Override
            public void destroyed() {
                super.destroyed();
                throw new TestRuntimeException("destroyed rt exception");
            }
        };

        try (LifecycleInjector injector = TestSupport.inject(listener)) {
        } finally {
            assertThat(listener.events, equalTo(Arrays.asList(Events.Injected, Events.Initialized, Events.Started, Events.Stopped, Events.Destroyed)));
        }
    }

    

    @Test(expected=AssertionError.class)
    public void assertionErrorInInject() {
        TrackingLifecycleListener listener = new TrackingLifecycleListener(name.getMethodName()) {
            @Inject
            @Override
            public void injected(Injector injector) {
                super.injected(injector);
                fail("injected exception");
            }
        };
        try (LifecycleInjector injector = TestSupport.inject(listener)) {
            fail("expected error provisioning injector");
        } catch (Exception e) {
            fail("expected AssertionError provisioning injector but got " + e);
        }
        finally {
            assertThat(listener.events, equalTo(
                Arrays.asList(Events.Injected, Events.Initialized, Events.Stopped, Events.Error)));
        }
    }
    
    
    @Test(expected=AssertionError.class)
    public void assertionErrorInPostConstruct() {
        TrackingLifecycleListener listener = new TrackingLifecycleListener(name.getMethodName()) {
            @PostConstruct
            @Override
            public void initialized() {
                super.initialized();
                fail("postconstruct exception");
            }
        };
        try (LifecycleInjector injector = TestSupport.inject(listener)) {
            fail("expected error creating injector");
        } catch (Exception e) {
            fail("expected AssertionError destroying injector but got " + e);
        }
        finally {
            assertThat(listener.events, equalTo(
                Arrays.asList(Events.Injected, Events.Initialized, Events.Stopped, Events.Error)));
        }
    }
    
    @Test(expected=AssertionError.class)
    public void assertionErrorInOnStarted() {
        TrackingLifecycleListener listener = new TrackingLifecycleListener(name.getMethodName()) {
            @Override
            public void onStarted() {
                super.onStarted();
                fail("onStarted exception");
            }
        };
        try (LifecycleInjector injector = TestSupport.inject(listener)) {
            fail("expected AssertionError starting injector");
        } catch (Exception e) {
            fail("expected AssertionError starting injector but got " + e);
        }
        finally {
            assertThat(listener.events, equalTo(
                Arrays.asList(Events.Injected, Events.Initialized, Events.Started, Events.Stopped, Events.Error)));
        }
    }    

    
    
    @Test(expected=AssertionError.class)
    public void assertionErrorInOnStopped() {
        TrackingLifecycleListener listener = new TrackingLifecycleListener(name.getMethodName()) {
            @Override
            public void onStopped(Throwable t) {
                super.onStopped(t);
                fail("onstopped exception");
            }
        };
        try (LifecycleInjector injector = TestSupport.inject(listener)) {
            fail("expected AssertionError stopping injector");
        } catch (Exception e) {
            fail("expected AssertionError stopping injector but got " + e);
        }
        finally {
            assertThat(listener.events, equalTo(
                Arrays.asList(Events.Injected, Events.Initialized, Events.Stopped, Events.Error)));
        }
    }   
    
    @Test(expected=AssertionError.class)
    public void assertionErrorInPreDestroy() {
        TrackingLifecycleListener listener = new TrackingLifecycleListener(name.getMethodName()) {
            @PreDestroy
            @Override
            public void destroyed() {
                super.destroyed();
                fail("expected exception from predestroy");
            }
        };
        try {
            TestSupport.inject(listener).close();
        } catch (Exception e) {
            e.printStackTrace();
            fail("expected no exceptions for failed destroy method but got " + e);
        }
        finally {
            assertThat(listener.events, equalTo(
                Arrays.asList(Events.Injected, Events.Initialized, Events.Started, Events.Stopped, Events.Destroyed)));
        }

    }

}
