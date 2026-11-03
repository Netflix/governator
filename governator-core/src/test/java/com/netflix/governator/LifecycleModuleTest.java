package com.netflix.governator;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Provider;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.CreationException;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.name.Names;
import com.google.inject.Scopes;
import com.netflix.governator.spi.LifecycleListener;

public class LifecycleModuleTest {
    private static final Logger logger = LoggerFactory.getLogger(LifecycleModuleTest.class);

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

    static class CountingSource {
        final AtomicInteger calls = new AtomicInteger();
        final String description;

        CountingSource(String description) {
            this.description = description;
        }

        @Override
        public String toString() {
            calls.incrementAndGet();
            return description;
        }
    }

    @Test
    public void checkBindingSourceOnceForRepeatedProvisions() {
        final CountingSource source = new CountingSource("application binding");
        try (LifecycleInjector injector = InjectorBuilder.fromModule(new AbstractModule() {
            @Override
            protected void configure() {
                binder().withSource(source).bind(Object.class).toProvider(Object::new);
            }
        }).createInjector()) {
            for (int i = 0; i < 10; i++) {
                injector.getInstance(Object.class);
            }
            Assert.assertEquals(1, source.calls.get());
        }
    }

    @Test
    public void leaveSpringManagedLifecycleToSpring() {
        final TrackingLifecycleListener normal = new TrackingLifecycleListener("normal");
        final TrackingLifecycleListener spring = new TrackingLifecycleListener("spring");
        final Key<TrackingLifecycleListener> normalKey =
                Key.get(TrackingLifecycleListener.class, Names.named("normal"));
        final Key<TrackingLifecycleListener> springKey =
                Key.get(TrackingLifecycleListener.class, Names.named("spring"));

        try (LifecycleInjector injector = InjectorBuilder.fromModule(new AbstractModule() {
            @Override
            protected void configure() {
                binder().withSource("application binding").bind(normalKey).toProvider(() -> normal);
                binder().withSource("generated spring-guice binding").bind(springKey).toProvider(() -> spring);
            }
        }).createInjector()) {
            injector.getInstance(normalKey);
            injector.getInstance(springKey);
            Assert.assertTrue(normal.events.contains(Events.Initialized));
            Assert.assertFalse(spring.events.contains(Events.Initialized));
        }
        Assert.assertTrue(normal.events.contains(Events.Destroyed));
        Assert.assertFalse(spring.events.contains(Events.Destroyed));
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
            @PreDestroy
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
    
    
    public static class Listener1 implements LifecycleListener {
        boolean wasStarted;
        boolean wasStopped;
        
        @Inject
        Provider<Listener2> nestedListener;
        
        @Override
        public void onStarted() {
            logger.info("starting listener1");   
            wasStarted = true;
            nestedListener.get();
        }

        @Override
        public void onStopped(Throwable error) {
            logger.info("stopped listener1");
            wasStopped = true;
            Assert.assertTrue(nestedListener.get().wasStopped);

        }
    }
    
    public static class Listener2 implements LifecycleListener {
        boolean wasStarted;
        boolean wasStopped;
        @Override
        public void onStarted() {
            logger.info("starting listener2");    
            wasStarted = true;
        }

        @Override
        public void onStopped(Throwable error) {
            logger.info("stopped listener2");
            wasStopped = true;
        }
        
    }

    @Test
    public void testNestedLifecycleListeners() {
        Listener1 listener1;
        Listener2 listener2;
        try (LifecycleInjector injector = InjectorBuilder.fromModule(new AbstractModule() {
            
            @Override
            protected void configure() {
                bind(Listener1.class).asEagerSingleton();
                bind(Listener2.class).in(Scopes.SINGLETON);                
            }
        }).createInjector()) {
            listener1 = injector.getInstance(Listener1.class);
            listener2 = listener1.nestedListener.get();
            Assert.assertNotNull(listener2);
            Assert.assertTrue(listener1.wasStarted);
            Assert.assertTrue(listener2.wasStarted);
        }
        Assert.assertTrue(listener1.wasStopped);
        Assert.assertTrue(listener2.wasStopped);
    }

}
