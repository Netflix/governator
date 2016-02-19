package com.netflix.governator.event;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Before;
import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.event.guava.GuavaApplicationEventModule;

public class ApplicationEventModuleTest {

    private Injector injector;

    @Before
    public void setup() {
        injector = InjectorBuilder.fromModules(new GuavaApplicationEventModule(), new AbstractModule() {
            @Override
            protected void configure() {
                bind(TestAnnotatedListener.class).toInstance(new TestAnnotatedListener());
                bind(TestListenerInterface.class).toInstance(new TestListenerInterface());
            }
        }).createInjector();
    }

    @Test
    public void testProvidedComponentsPresent() {
        ApplicationEventDispatcher dispatcher = injector.getInstance(ApplicationEventDispatcher.class);
        TestAnnotatedListener listener = injector.getInstance(TestAnnotatedListener.class);
        TestListenerInterface listenerInterface = injector.getInstance(TestListenerInterface.class);
        assertNotNull(dispatcher);
        assertNotNull(listener);
        assertNotNull(listenerInterface);
    }

    @Test
    public void testAnnotatedListener() throws Exception {
        ApplicationEventDispatcher dispatcher = injector.getInstance(ApplicationEventDispatcher.class);
        TestAnnotatedListener listener = injector.getInstance(TestAnnotatedListener.class);
        assertEquals(0, listener.invocationCount.get());
        dispatcher.publishEvent(new TestEvent());
        assertEquals(1, listener.invocationCount.get());
        dispatcher.publishEvent(new NotTestEvent());
        assertEquals(1, listener.invocationCount.get());
    }

    @Test
    public void testManuallyRegisteredApplicationEventListeners() throws Exception {
        ApplicationEventDispatcher dispatcher = injector.getInstance(ApplicationEventDispatcher.class);
        final AtomicInteger testEventCounter = new AtomicInteger();
        final AtomicInteger notTestEventCounter = new AtomicInteger();
        final AtomicInteger allEventCounter = new AtomicInteger();

        dispatcher.registerListener(TestEvent.class, new ApplicationEventListener<TestEvent>() {
            public void onEvent(TestEvent event) {
                testEventCounter.incrementAndGet();
            }
        });
        dispatcher.registerListener(NotTestEvent.class, new ApplicationEventListener<NotTestEvent>() {
            public void onEvent(NotTestEvent event) {
                notTestEventCounter.incrementAndGet();
            }
        });
        dispatcher.registerListener(ApplicationEvent.class, new ApplicationEventListener<ApplicationEvent>() {
            public void onEvent(ApplicationEvent event) {
                allEventCounter.incrementAndGet();
            }
        });

        dispatcher.publishEvent(new TestEvent());
        assertEquals(1, testEventCounter.get());
        assertEquals(0, notTestEventCounter.get());
        assertEquals(1, allEventCounter.get());
    }

    @Test
    public void testManuallyRegisteredApplicationEventListenersWithoutClassArgument() throws Exception {
        ApplicationEventDispatcher dispatcher = injector.getInstance(ApplicationEventDispatcher.class);
        final AtomicInteger testEventCounter = new AtomicInteger();
        final AtomicInteger notTestEventCounter = new AtomicInteger();
        final AtomicInteger allEventCounter = new AtomicInteger();

        dispatcher.registerListener(new ApplicationEventListener<TestEvent>() {
            public void onEvent(TestEvent event) {
                testEventCounter.incrementAndGet();
            }
        });
        dispatcher.registerListener(new ApplicationEventListener<NotTestEvent>() {
            public void onEvent(NotTestEvent event) {
                notTestEventCounter.incrementAndGet();
            }
        });
        dispatcher.registerListener(new ApplicationEventListener<ApplicationEvent>() {
            public void onEvent(ApplicationEvent event) {
                allEventCounter.incrementAndGet();
            }
        });

        dispatcher.publishEvent(new TestEvent());
        assertEquals(1, testEventCounter.get());
        assertEquals(0, notTestEventCounter.get());
        assertEquals(1, allEventCounter.get());
    }
    
    @Test
    public void testInjectorDiscoveredApplicationEventListeners() throws Exception {
        ApplicationEventDispatcher dispatcher = injector.getInstance(ApplicationEventDispatcher.class);
        TestListenerInterface listener = injector.getInstance(TestListenerInterface.class);
        assertEquals(0, listener.invocationCount.get());
        dispatcher.publishEvent(new TestEvent());
        assertEquals(1, listener.invocationCount.get());
        dispatcher.publishEvent(new NotTestEvent());
        assertEquals(1, listener.invocationCount.get());
    }
    
    @Test
    public void testUnregisterApplicationEventListener() throws Exception {
        ApplicationEventDispatcher dispatcher = injector.getInstance(ApplicationEventDispatcher.class);
        final AtomicInteger testEventCounter = new AtomicInteger();

        ApplicationEventRegistration registration = dispatcher.registerListener(new ApplicationEventListener<TestEvent>() {
            public void onEvent(TestEvent event) {
                testEventCounter.incrementAndGet();
            }
        });
        
        dispatcher.publishEvent(new TestEvent());
        assertEquals(1, testEventCounter.get());
        registration.unregister();
        assertEquals(1, testEventCounter.get());        
    }

    private class TestAnnotatedListener {
        AtomicInteger invocationCount = new AtomicInteger();

        @EventListener
        public void doThing(TestEvent event) {
            invocationCount.incrementAndGet();
        }

        @EventListener
        public void doNothing(String invalidArgumentType) {
            fail("This should never be called");
        }

        @EventListener
        public void doNothing(Class<Object> arg1, Object arg2) {
            fail("This should never be called");
        }
    }
    
    private class TestListenerInterface implements ApplicationEventListener<TestEvent> {
        AtomicInteger invocationCount = new AtomicInteger();

        @Override
        public void onEvent(TestEvent event) {
            invocationCount.incrementAndGet();
        }
    }

    private class TestEvent implements ApplicationEvent {

    }

    private class NotTestEvent implements ApplicationEvent {

    }

}
