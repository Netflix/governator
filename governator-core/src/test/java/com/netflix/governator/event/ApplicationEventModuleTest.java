package com.netflix.governator.event;

import static org.junit.Assert.*;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.netflix.governator.InjectorBuilder;

public class ApplicationEventModuleTest {
    
    @Test
    public void testEventBus() throws Exception {
        Injector injector = InjectorBuilder.fromModules(new ApplicationEventModule(), new AbstractModule() {
                        
            @Override
            protected void configure() {
                bind(TestListener.class).toInstance(new TestListener());;
            }
        }).createInjector();
        
        ApplicationEventPublisher publisher = injector.getInstance(ApplicationEventPublisher.class);
        TestListener listener = injector.getInstance(TestListener.class);
        assertNotNull(publisher);
        assertNotNull(listener);
        assertEquals(0, listener.invocationCount.get());
        publisher.publishEvent(new TestEvent());
        assertEquals(1, listener.invocationCount.get());
       
    }
    
    private class TestListener {
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
    
    private class TestEvent implements ApplicationEvent {

        
    }

}
