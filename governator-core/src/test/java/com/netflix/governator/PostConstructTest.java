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

    private static class PostConstructParent1 {
        @PostConstruct
        public void init() {
            System.out.println("parent.init");
        }
    }

    private static class PostConstructChild1 extends PostConstructParent1 {
        @PostConstruct
        public void init() {
            System.out.println("child.init");
        }
    }
    
    private static class PostConstructParent2 {
        @PostConstruct
        public void anotherInit() {
            System.out.println("parent.anotherInit");
        }
    }

    private static class PostConstructChild2 extends PostConstructParent2 {
        @PostConstruct
        public void init() {
            System.out.println("child.init");
        }
    }
    
    private static class PostConstructParent3 {
        @PostConstruct
        public void init() {

        }
    }

    static class PostConstructChild3 extends PostConstructParent3 {
        public void init() {
            System.out.println("init invoked");
        }
    }

    
    static class MultiplePostConstructs {
        @PostConstruct
        public void init1() {
            System.out.println("init1");
        }
        
        @PostConstruct
        public void init2() {
            System.out.println("init2");
        }       
    }
    

    @Test
    public void testLifecycleInitInheritance1() {
        final PostConstructChild1 postConstructChild = Mockito.mock(PostConstructChild1.class);
        InOrder inOrder = Mockito.inOrder(postConstructChild);

        try (LifecycleInjector injector = TestSupport.inject(postConstructChild)) {
            Assert.assertNotNull(injector.getInstance(postConstructChild.getClass()));
            // not twice
            inOrder.verify(postConstructChild, Mockito.times(1)).init();
        }
    }

    @Test
    public void testLifecycleInitInheritance2() {
        final PostConstructChild2 postConstructChild = Mockito.mock(PostConstructChild2.class);
        InOrder inOrder = Mockito.inOrder(postConstructChild);

        try (LifecycleInjector injector = TestSupport.inject(postConstructChild)) {
            Assert.assertNotNull(injector.getInstance(postConstructChild.getClass()));
            // parent postConstruct before child postConstruct
            inOrder.verify(postConstructChild, Mockito.times(1)).anotherInit();
            // not twice
            inOrder.verify(postConstructChild, Mockito.times(1)).init();
        }
    }
    
    @Test
    public void testLifecycleShutdownInheritance3() {
        final PostConstructChild3 postConstructChild = Mockito.spy(new PostConstructChild3());
        InOrder inOrder = Mockito.inOrder(postConstructChild);

        try (LifecycleInjector injector = TestSupport.inject(postConstructChild)) {
            Assert.assertNotNull(injector.getInstance(postConstructChild.getClass()));
            Mockito.verify(postConstructChild, Mockito.never()).init();
        }
        // never, child class overrides method without annotation
        inOrder.verify(postConstructChild, Mockito.never()).init(); 
    }
    

    @Test
    public void testLifecycleMultipleAnnotations() {
        final MultiplePostConstructs multiplePostConstructs = Mockito.spy(new MultiplePostConstructs());
        try (LifecycleInjector injector = new TestSupport()
                .withFeature(GovernatorFeatures.STRICT_JSR250_VALIDATION, true)
                .withSingleton(multiplePostConstructs)
                .inject()) {
            Assert.assertNotNull(injector.getInstance(multiplePostConstructs.getClass()));
            Mockito.verify(multiplePostConstructs, Mockito.never()).init1();
            Mockito.verify(multiplePostConstructs, Mockito.never()).init2();
        }
        // never, multiple annotations should be ignored
        Mockito.verify(multiplePostConstructs, Mockito.never()).init1(); 
        Mockito.verify(multiplePostConstructs, Mockito.never()).init2(); 
    }    
    

    @Test
    public void testLifecycleInitWithInvalidPostConstructs() {
        InvalidPostConstructs mockInstance = Mockito.mock(InvalidPostConstructs.class);
        try (LifecycleInjector injector = new TestSupport()
                .withFeature(GovernatorFeatures.STRICT_JSR250_VALIDATION, true)
                .withSingleton(mockInstance)
                .inject()) {
            Assert.assertNotNull(injector.getInstance(InvalidPostConstructs.class));
            Mockito.verify(mockInstance, Mockito.never()).initWithParameters(Mockito.anyString());
            Mockito.verify(mockInstance, Mockito.never()).initWithReturnValue();
        }
    }
    
    @Test
    public void testLifecycleInitWithPostConstructException() {
        InvalidPostConstructs mockInstance = Mockito.mock(InvalidPostConstructs.class);
        try (LifecycleInjector injector = new TestSupport()
                .withFeature(GovernatorFeatures.STRICT_JSR250_VALIDATION, true)
                .withSingleton(mockInstance)
                .inject()) {
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

}
