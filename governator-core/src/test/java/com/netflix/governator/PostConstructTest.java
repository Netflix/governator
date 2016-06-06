package com.netflix.governator;

import javax.annotation.PostConstruct;
import javax.inject.Singleton;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;

import junit.framework.Assert;

public class PostConstructTest {
    private static class SimplePostConstruct {
        @PostConstruct
        public void init() {

        }
    }

    private static class InvalidPostConstructs {
        @PostConstruct
        public String initWithReturnValue() {
            return "invalid return type";
        }

        @PostConstruct
        public static void initStatic() {
            // can't use static method type
            throw new RuntimeException("boom");
        }

        @PostConstruct
        public void initWithParameters(String invalidArg) {
            // can't use method parameters
        }
    }

    private static class PostConstructParent {
        @PostConstruct
        public void init() {
            System.out.println("parent.init");
        }

        @PostConstruct
        public void anotherInit() {
            System.out.println("parent.anotherInit");
        }

        @PostConstruct
        public void yetAnotherInit() {
            System.out.println("parent.yetAnotherInit");
        }

    }

    private static class PostConstructChild extends PostConstructParent {
        @PostConstruct
        public void init() {
            System.out.println("child.init");
        }

        public void yetAnotherInit() {
            System.out.println("child.yetAnotherInit");
        }
    }

    @Test
    public void testLifecycleInitInheritance() {
        final PostConstructChild postConstructChild = Mockito.mock(PostConstructChild.class);
        InOrder inOrder = Mockito.inOrder(postConstructChild);

        try (LifecycleInjector injector = TestSupport.inject(postConstructChild)) {
            Assert.assertNotNull(injector.getInstance(PostConstructChild.class));
            // parent postConstruct before child postConstruct
            inOrder.verify(postConstructChild, Mockito.times(1)).anotherInit();
            // not twice
            inOrder.verify(postConstructChild, Mockito.times(1)).init();
        }
    }

    @Test
    public void testLifecycleInitWithInvalidPostConstructs() {
        InvalidPostConstructs mockInstance = Mockito.mock(InvalidPostConstructs.class);
        try (LifecycleInjector injector = TestSupport.inject(mockInstance)) {
            Assert.assertNotNull(injector.getInstance(InvalidPostConstructs.class));
            Mockito.verify(mockInstance, Mockito.never()).initWithParameters(Mockito.anyString());
            Mockito.verify(mockInstance, Mockito.never()).initWithReturnValue();
        }
    }

    @Test
    public void testLifecycleInit() {
        SimplePostConstruct mockInstance = Mockito.mock(SimplePostConstruct.class);
        try (LifecycleInjector injector = TestSupport.inject(mockInstance)) {
            Assert.assertNotNull(injector.getInstance(SimplePostConstruct.class));
            Mockito.verify(mockInstance, Mockito.times(1)).init();
        }
    }

    @Test
    public void testLifecycleInitWithAtProvides() {
        final SimplePostConstruct simplePostConstruct = Mockito.mock(SimplePostConstruct.class);

        InjectorBuilder builder = InjectorBuilder.fromModule(new AbstractModule() {
            @Override
            protected void configure() {
            }

            @Provides
            @Singleton
            SimplePostConstruct getSimplePostConstruct() {
                return simplePostConstruct;
            }
        });
        try (LifecycleInjector injector = builder.createInjector()) {
            Mockito.verify(injector.getInstance(SimplePostConstruct.class), Mockito.times(1)).init();
        }
    }

    @Before
    public void printTestHeader() {
        System.out.println("\n=======================================================");
        System.out.println("  Running Test : " + name.getMethodName());
        System.out.println("=======================================================\n");
    }

    @Rule
    public TestName name = new TestName();
    
    interface TestInt {
        @PostConstruct
        public void foo();
    }
    
    private static class T implements Runnable, TestInt {
        @Override
        public void run() {
            
        }
        
        @Override
        public String toString() {
            return "yay!";
        }
        
        @Override
        public void foo() {
            
        }

    }

}
