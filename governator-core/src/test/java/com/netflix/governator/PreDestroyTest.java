package com.netflix.governator;

import java.io.Closeable;
import java.io.IOException;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.InOrder;
import org.mockito.Mockito;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.name.Named;
import com.google.inject.name.Names;

public class PreDestroyTest {
    private static final int GC_SLEEP_TIME = 100;

    private static class Foo {
        private volatile boolean shutdown = false;
        
        Foo() {
            System.out.println("Foo constructed: " + this);
        }
        
        @PreDestroy
        protected void shutdown() {
            shutdown = true;
        }
        
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public String toString() {
            return "Foo@" + System.identityHashCode(this);
        }
    }

    @ThreadLocalScoped
    private static class AnnotatedFoo {
        private volatile boolean shutdown = false;
        
        @SuppressWarnings("unused")
        AnnotatedFoo() {
            System.out.println("AnnotatedFoo constructed: " + this);
        }
        
        @PreDestroy
        public void shutdown() {
            this.shutdown = true;
        }
        
        public boolean isShutdown() {
            return shutdown;
        }

        @Override
        public String toString() {
            return "AnnotatedFoo@" + System.identityHashCode(this);
        }
    }    
    
    private static class InvalidPreDestroys {
        @PreDestroy
        public String shutdownWithReturnValue() {
            return "invalid return type";
        }

        @PreDestroy
        public static void shutdownStatic() {
            // can't use static method type
            throw new RuntimeException("boom");
        }

        @PreDestroy
        public void shutdownWithParameters(String invalidArg) {
            // can't use method parameters
        }
    }

    private interface PreDestroyInterface {
        @PreDestroy
        public void destroy();
    }

    private static class PreDestroyImpl implements PreDestroyInterface {
        @Override
        public void destroy() {
            // should not be called
        }
    }

    private static class RunnableType implements Runnable {
        @Override
        @PreDestroy
        public void run() {
            // method from interface; will it be called?
        }
    }

    private static class CloseableType implements Closeable {
        @Override
        public void close() throws IOException {
        }

        @PreDestroy
        public void shutdown() {

        }
    }

    private static class PreDestroyParent1 {
        @PreDestroy
        public void shutdown() {

        }
    }

    private static class PreDestroyChild1 extends PreDestroyParent1 {
        @PreDestroy
        public void shutdown() {
            System.out.println("shutdown invoked");
        }
    }
    
    private static class PreDestroyParent2 {
        @PreDestroy
        public void anotherShutdown() {

        }
    }

    private static class PreDestroyChild2 extends PreDestroyParent2 {
        @PreDestroy
        public void shutdown() {
            System.out.println("shutdown invoked");
        }
    }
    
    private static class PreDestroyParent3 {
        @PreDestroy
        public void shutdown() {

        }
    }

    private static class PreDestroyChild3 extends PreDestroyParent3 {
        public void shutdown() {
            System.out.println("shutdown invoked");
        }
    }
    
    private static class MultipleDestroys  {
        @PreDestroy
        public void shutdown1() {
            System.out.println("shutdown1 invoked");
        }
        @PreDestroy
        public void shutdown2() {
            System.out.println("shutdown2 invoked");
        }
    }
    
    
    private static class EagerBean {
        volatile boolean shutdown = false;
        SingletonBean singletonInstance;
        @Inject
        public EagerBean(SingletonBean singletonInstance) {
            this.singletonInstance = singletonInstance;
        }
        
        @PreDestroy
        public void shutdown() {
            System.out.println("eager bean shutdown invoked");
            shutdown = true;
            this.singletonInstance.eagerShutdown = true;
        }       
    }
    
    @Singleton
    private static class SingletonBean {
        volatile boolean eagerShutdown = false;
        boolean shutdown = false;
        
        @PreDestroy
        public void shutdown() {
            System.out.println("singleton bean shutdown invoked");
            shutdown = true;
            Assert.assertTrue(eagerShutdown);
        }       
    }
   
    
    @Test
    public void testEagerSingletonShutdown() {
        EagerBean eagerBean;
        SingletonBean singletonBean;
        try (LifecycleInjector injector = InjectorBuilder.fromModule(new AbstractModule() {            
            @Override
            protected void configure() {
                bind(EagerBean.class).asEagerSingleton();
                bind(SingletonBean.class).in(Scopes.SINGLETON);
            }}).createInjector()) {
            eagerBean = injector.getInstance(EagerBean.class);
            singletonBean = injector.getInstance(SingletonBean.class);
            Assert.assertFalse(eagerBean.shutdown);
            Assert.assertFalse(singletonBean.shutdown);
        }
        Assert.assertTrue(eagerBean.shutdown);
        Assert.assertTrue(singletonBean.shutdown);
    }
    
    
    @Test
    public void testLifecycleShutdownInheritance1() {
        final PreDestroyChild1 preDestroyChild = Mockito.spy(new PreDestroyChild1());
        InOrder inOrder = Mockito.inOrder(preDestroyChild);

        try (LifecycleInjector injector = TestSupport.inject(preDestroyChild)) {
            Assert.assertNotNull(injector.getInstance(preDestroyChild.getClass()));
            Mockito.verify(preDestroyChild, Mockito.never()).shutdown();
        }
        // once not twice
        inOrder.verify(preDestroyChild, Mockito.times(1)).shutdown(); 
    }

    @Test
    public void testLifecycleShutdownInheritance2() {
        final PreDestroyChild2 preDestroyChild = Mockito.spy(new PreDestroyChild2());
        InOrder inOrder = Mockito.inOrder(preDestroyChild);

        try (LifecycleInjector injector = TestSupport.inject(preDestroyChild)) {
            Assert.assertNotNull(injector.getInstance(preDestroyChild.getClass()));
            Mockito.verify(preDestroyChild, Mockito.never()).shutdown();
        }
        // once not twice
        inOrder.verify(preDestroyChild, Mockito.times(1)).shutdown(); 
        inOrder.verify(preDestroyChild, Mockito.times(1)).anotherShutdown(); 
    }
    
    @Test
    public void testLifecycleShutdownInheritance3() {
        final PreDestroyChild3 preDestroyChild = Mockito.spy(new PreDestroyChild3());
        InOrder inOrder = Mockito.inOrder(preDestroyChild);

        try (LifecycleInjector injector = TestSupport.inject(preDestroyChild)) {
            Assert.assertNotNull(injector.getInstance(preDestroyChild.getClass()));
            Mockito.verify(preDestroyChild, Mockito.never()).shutdown();
        }
        // never, child class overrides method without annotation
        inOrder.verify(preDestroyChild, Mockito.never()).shutdown(); 
    }
    
    @Test
    public void testLifecycleMultipleAnnotations() {
        final MultipleDestroys multipleDestroys = Mockito.spy(new MultipleDestroys());

        try (LifecycleInjector injector = new TestSupport()
                .withFeature(GovernatorFeatures.STRICT_JSR250_VALIDATION, true)
                .withSingleton(multipleDestroys)
                .inject()) {
            Assert.assertNotNull(injector.getInstance(multipleDestroys.getClass()));
            Mockito.verify(multipleDestroys, Mockito.never()).shutdown1();
            Mockito.verify(multipleDestroys, Mockito.never()).shutdown2();
        }
        // never, multiple annotations should be ignored
        Mockito.verify(multipleDestroys, Mockito.never()).shutdown1(); 
        Mockito.verify(multipleDestroys, Mockito.never()).shutdown2(); 
    }
    
    @Test
    public void testLifecycleDeclaredInterfaceMethod() {
        final RunnableType runnableInstance = Mockito.mock(RunnableType.class);
        InOrder inOrder = Mockito.inOrder(runnableInstance);

        try (LifecycleInjector injector = TestSupport.inject(runnableInstance)) {
            Assert.assertNotNull(injector.getInstance(RunnableType.class));
            Mockito.verify(runnableInstance, Mockito.never()).run();
        }
        inOrder.verify(runnableInstance, Mockito.times(1)).run();
    }

    @Test
    public void testLifecycleAnnotatedInterfaceMethod() {
        final PreDestroyImpl impl = Mockito.mock(PreDestroyImpl.class);
        InOrder inOrder = Mockito.inOrder(impl);

        try (LifecycleInjector injector = TestSupport.inject(impl)) {
            Assert.assertNotNull(injector.getInstance(RunnableType.class));
            Mockito.verify(impl, Mockito.never()).destroy();
        }
        inOrder.verify(impl, Mockito.never()).destroy();
    }

    @Test
    public void testLifecycleShutdownWithInvalidPreDestroys() {
        final InvalidPreDestroys ipd = Mockito.mock(InvalidPreDestroys.class);

        try (LifecycleInjector injector = new TestSupport()
                .withFeature(GovernatorFeatures.STRICT_JSR250_VALIDATION, true)
                .withSingleton(ipd)
                .inject()) {
            Assert.assertNotNull(injector.getInstance(InvalidPreDestroys.class));
            Mockito.verify(ipd, Mockito.never()).shutdownWithParameters(Mockito.anyString());
            Mockito.verify(ipd, Mockito.never()).shutdownWithReturnValue();
        }
        Mockito.verify(ipd, Mockito.never()).shutdownWithParameters(Mockito.anyString());
        Mockito.verify(ipd, Mockito.never()).shutdownWithReturnValue();
    }

    @Test
    public void testLifecycleCloseable() {
        final CloseableType closeableType = Mockito.mock(CloseableType.class);
        try {
            Mockito.doThrow(new IOException("boom")).when(closeableType).close();
        } catch (IOException e1) {
            // ignore, mock only
        }

        try (LifecycleInjector injector = TestSupport.inject(closeableType)) {
            Assert.assertNotNull(injector.getInstance(closeableType.getClass()));
            try {
                Mockito.verify(closeableType, Mockito.never()).close();
            } catch (IOException e) {
                // close() called before shutdown and failed
                Assert.fail("close() called before shutdown and  failed");
            }
        }

        try {
            Mockito.verify(closeableType, Mockito.times(1)).close();
            Mockito.verify(closeableType, Mockito.never()).shutdown();
        } catch (IOException e) {
            // close() called before shutdown and failed
            Assert.fail("close() called after shutdown and  failed");
        }

    }

    @Test
    public void testLifecycleShutdown() {
        final Foo foo = Mockito.mock(Foo.class);
        try (LifecycleInjector injector = TestSupport.inject(foo)) {
            Assert.assertNotNull(injector.getInstance(foo.getClass()));
            Mockito.verify(foo, Mockito.never()).shutdown();
        }
        Mockito.verify(foo, Mockito.times(1)).shutdown();
    }

    @Test
    public void testLifecycleShutdownWithAtProvides() {
        InjectorBuilder builder = InjectorBuilder.fromModule(new AbstractModule() {
            @Override
            protected void configure() {
            }

            @Provides
            @Singleton
            Foo getFoo() {
                return new Foo();
            }
        });

        Foo managedFoo = null;
        try (LifecycleInjector injector = builder.createInjector()) {
            managedFoo = injector.getInstance(Foo.class);
            Assert.assertNotNull(managedFoo);
            Assert.assertFalse(managedFoo.isShutdown());
        }
        managedFoo = null;
        builder = null;
    }
    
    @Test
    public void testLifecycleShutdownWithExplicitScope() throws Exception {
        final ThreadLocalScope threadLocalScope = new ThreadLocalScope();
        
        InjectorBuilder builder = InjectorBuilder.fromModule(new AbstractModule() {
            @Override
            protected void configure() {
                binder().bind(Foo.class).in(threadLocalScope);
            }
        });

        Foo managedFoo = null;
        try (LifecycleInjector injector = builder.createInjector()) {
            threadLocalScope.enter();
            managedFoo = injector.getInstance(Foo.class);
            Assert.assertNotNull(managedFoo);
            Assert.assertFalse(managedFoo.isShutdown());
            threadLocalScope.exit();
            
            System.gc();
            Thread.sleep(GC_SLEEP_TIME);
            Assert.assertTrue(managedFoo.isShutdown());
        }
    }

    @Test
    public void testLifecycleShutdownWithAnnotatedExplicitScope() throws Exception {
        final ThreadLocalScope threadLocalScope = new ThreadLocalScope();
        
        InjectorBuilder builder = InjectorBuilder.fromModules(new AbstractModule() {
            @Override
            protected void configure() {
                binder().bind(Key.get(AnnotatedFoo.class));
            }
        },
         new AbstractModule() {
            @Override
            protected void configure() {
                binder().bindScope(ThreadLocalScoped.class, threadLocalScope);
            }
        });

        AnnotatedFoo managedFoo = null;
        try (LifecycleInjector injector = builder.createInjector()) {
            threadLocalScope.enter();
            managedFoo = injector.getInstance(AnnotatedFoo.class);
            Assert.assertNotNull(managedFoo);
            Assert.assertFalse(managedFoo.shutdown);
            threadLocalScope.exit();
            
            System.gc();
            Thread.sleep(GC_SLEEP_TIME);
            synchronized(managedFoo) {
            Assert.assertTrue(managedFoo.shutdown);
            }
        }
    }
    
    
    @Test
    public void testLifecycleShutdownWithMultipleInScope() throws Exception {
        final ThreadLocalScope scope = new ThreadLocalScope();
        InjectorBuilder builder = InjectorBuilder.fromModule(new AbstractModule() {
            @Override
            protected void configure() {
                binder().bindScope(ThreadLocalScoped.class, scope);
            }

            @Provides
            @ThreadLocalScoped
            @Named("afoo1")
            protected AnnotatedFoo afoo1() {
                return new AnnotatedFoo();
            }
            
            @Provides
            @ThreadLocalScoped
            @Named("afoo2")
            protected AnnotatedFoo afoo2() {
                return new AnnotatedFoo();
            }
        });

        AnnotatedFoo managedFoo1 = null;
        AnnotatedFoo managedFoo2 = null;
        try (LifecycleInjector injector = builder.createInjector()) {
            scope.enter();
            managedFoo1 = injector.getInstance(Key.get(AnnotatedFoo.class, Names.named("afoo1")));
            Assert.assertNotNull(managedFoo1);
            Assert.assertFalse(managedFoo1.isShutdown());     

            managedFoo2 = injector.getInstance(Key.get(AnnotatedFoo.class, Names.named("afoo2")));
            Assert.assertNotNull(managedFoo2);
            Assert.assertFalse(managedFoo2.isShutdown());
            
            scope.exit();
            System.gc();
            Thread.sleep(GC_SLEEP_TIME);
            
            Assert.assertTrue(managedFoo1.isShutdown());
            Assert.assertTrue(managedFoo2.isShutdown());
        }
    }    
    
    
    @Test
    public void testLifecycleShutdownWithSingletonScope() throws Exception {
        InjectorBuilder builder = InjectorBuilder.fromModule(new AbstractModule() {
            @Override
            protected void configure() {
                binder().bind(Foo.class).in(Scopes.SINGLETON);
            }
        });

        Foo managedFoo = null;
        try (LifecycleInjector injector = builder.createInjector()) {
            managedFoo = injector.getInstance(Foo.class);
            Assert.assertNotNull(managedFoo);
            Assert.assertFalse(managedFoo.isShutdown());
            
        }
        System.gc();
        Thread.sleep(GC_SLEEP_TIME);
        Assert.assertTrue(managedFoo.isShutdown());
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
