package com.netflix.governator;

import java.util.concurrent.atomic.AtomicBoolean;

import javax.inject.Inject;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.netflix.governator.visitors.BindingTracingVisitor;
import com.netflix.governator.visitors.KeyTracingVisitor;
import com.netflix.governator.visitors.ModuleSourceTracingVisitor;

public class ModuleBuilderTest {
    private static Logger LOG = LoggerFactory.getLogger(ModuleBuilderTest.class);
    
    @Test
    public void testLifecycleInjectorEvents() {
        final AtomicBoolean injectCalled = new AtomicBoolean(false);
        final AtomicBoolean afterInjectorCalled = new AtomicBoolean(false);
        final AtomicBoolean beforeInjectorCalled = new AtomicBoolean(false);
        
        ModuleBuilder
            .createDefault()
            .createInjector(new LifecycleInjectorCreator() {
                @Inject
                public void initialize() {
                    injectCalled.set(true);
                }
                
                @Override
                protected void onAfterInjectorCreated() {
                    afterInjectorCalled.set(true);
                }

                @Override
                protected void onBeforeInjectorCreated() {
                    beforeInjectorCalled.set(true);
                }
            })
            .shutdown();
        
        Assert.assertTrue(injectCalled.get());
        Assert.assertTrue(afterInjectorCalled.get());
        Assert.assertTrue(beforeInjectorCalled.get());
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
        ModuleBuilder
            .fromModule(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(String.class).toInstance("Hello world");
                }
            })
            .forEachElement(new BindingTracingVisitor(name.getMethodName()))
            .createInjector();
    }
    
    @Test
    public void testKeyTracing() {
        ModuleBuilder
            .fromModule(new AbstractModule() {
                @Override
                protected void configure() {
                    bind(String.class).toInstance("Hello world");
                }
            })
            .forEachElement(new KeyTracingVisitor(name.getMethodName()))
            .createInjector();
    }
    
    @Test
    public void testWarnOnStaticInjection() {
        ModuleBuilder
            .fromModule(new AbstractModule() {
                @Override
                protected void configure() {
                    this.requestStaticInjection(String.class);
                }
            })
            .warnOfStaticInjections()
            .createInjector();
    }
    
    @Test
    public void testStripStaticInjection() {
        ModuleBuilder
            .fromModule(new AbstractModule() {
                @Override
                protected void configure() {
                    this.requestStaticInjection(String.class);
                }
            })
            .info("Stripping static injections")
            .stripStaticInjections()
            .warnOfStaticInjections()
            .createInjector();
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
        ModuleBuilder
            .fromModule(new ModuleA())
            .forEachElement(new ModuleSourceTracingVisitor())
            .createInjector();
    }
}
