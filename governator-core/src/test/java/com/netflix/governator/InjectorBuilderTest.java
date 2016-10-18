package com.netflix.governator;

import com.google.inject.AbstractModule;
import com.google.inject.spi.Element;
import com.netflix.governator.visitors.BindingTracingVisitor;
import com.netflix.governator.visitors.KeyTracingVisitor;
import com.netflix.governator.visitors.ModuleSourceTracingVisitor;
import com.netflix.governator.visitors.WarnOfToInstanceInjectionVisitor;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.Mockito;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

import javax.inject.Inject;

public class InjectorBuilderTest {
 
    
    @Test
    public void testLifecycleInjectorEvents() {
        final AtomicBoolean injectCalled = new AtomicBoolean(false);
        final AtomicBoolean afterInjectorCalled = new AtomicBoolean(false);
        
        InjectorBuilder
            .fromModule(new AbstractModule() {
                @Override
                protected void configure() {
                }
            })
            .createInjector(new LifecycleInjectorCreator() {
                @Inject
                public void initialize() {
                    injectCalled.set(true);
                }
                
                @Override
                protected void onCompletedInjectorCreate() {
                    afterInjectorCalled.set(true);
                }
            })
            .shutdown();
        
        Assert.assertTrue(injectCalled.get());
        Assert.assertTrue(afterInjectorCalled.get());
    }
    
    @Before
    public void printTestHeader() {
        System.out.println("\n=======================================================");
        System.out.println("  Running Test : " + name.getMethodName());
        System.out.println("=======================================================\n");
    }
    
    @Rule
    public TestName name = new TestName();
    
    @Test
    public void testBindingTracing() {
        InjectorBuilder
            .fromModule(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(String.class).toInstance("Hello world");
                }
            })
            .traceEachElement(new BindingTracingVisitor())
            .createInjector();
    }
    
    @Test
    public void testForEachBinding() {
        Consumer<String> consumer = Mockito.mock(Consumer.class);
        InjectorBuilder
            .fromModule(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(String.class).toInstance("Hello world");
                }
            })
            .forEachElement(new WarnOfToInstanceInjectionVisitor(), consumer)
            .createInjector();
        
        Mockito.verify(consumer, Mockito.times(1)).accept(Mockito.anyString());
    }
    
    @Test
    public void testKeyTracing() {
        try (LifecycleInjector li = InjectorBuilder
            .fromModule(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(String.class).toInstance("Hello world");
                }
            })
            .traceEachElement(new KeyTracingVisitor())
            .createInjector()) {}
    }
    
    @Test
    public void testWarnOnStaticInjection() {
        List<Element> elements = InjectorBuilder
            .fromModule(new AbstractModule() {
                @Override
                protected void configure() {
                    this.requestStaticInjection(String.class);
                }
            })
            .warnOfStaticInjections()
            .getElements();
        
        Assert.assertEquals(1, elements.size());
    }
    
    @Test
    public void testStripStaticInjection() {
        List<Element> elements = InjectorBuilder
            .fromModule(new AbstractModule() {
                @Override
                protected void configure() {
                    this.requestStaticInjection(String.class);
                }
            })
            .stripStaticInjections()
            .warnOfStaticInjections()
            .getElements();
        
        Assert.assertEquals(0, elements.size());
    }
    
    public static class ModuleA extends AbstractModule {
        @Override
        protected void configure() {
            install(new ModuleB());
            install(new ModuleC());
        }
    }
    
    public static class ModuleB extends AbstractModule {
        @Override
        protected void configure() {
            install(new ModuleC());
        }
    }
    
    public static class ModuleC extends AbstractModule {
        @Override
        protected void configure() {
            bind(String.class).toInstance("Hello world");
        }
        
        @Override
        public int hashCode() {
            return ModuleC.class.hashCode();
        }
        
        @Override
        public boolean equals(Object obj) {
            return obj.getClass().equals(getClass());
        }
    }
    
    @Test
    public void testTraceModules() {
        InjectorBuilder
            .fromModule(new ModuleA())
            .traceEachElement(new ModuleSourceTracingVisitor())
            .createInjector();
    }
}
