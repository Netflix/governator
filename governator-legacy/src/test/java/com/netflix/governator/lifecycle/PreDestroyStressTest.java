package com.netflix.governator.lifecycle;

import java.util.Arrays;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Named;

import org.apache.log4j.Level;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.name.Names;
import com.netflix.governator.LifecycleManager;
import com.netflix.governator.guice.LifecycleInjector;

@RunWith(MockitoJUnitRunner.class)
public class PreDestroyStressTest {
    private final class ScopingModule extends AbstractModule {
        @Override
        protected void configure() {
            bindScope(LocalScoped.class, localScope);
        }

        @Provides
        @LocalScoped
        @Named("thing1")
        public LifecycleSubject thing1() {
            return new LifecycleSubject("thing1");
        }
    }


    static class LifecycleSubject {
        private Logger logger = LoggerFactory.getLogger(LifecycleSubject.class);
        private String name;
        private volatile boolean postConstructed = false;
        private volatile boolean preDestroyed = false;
        private byte[] bulk;

        private static AtomicInteger instanceCounter = new AtomicInteger(0);

        public LifecycleSubject(String name) {
            this.name = name;
            this.bulk = new byte[1024*500]; 
            Arrays.fill(bulk, (byte)0);        
            instanceCounter.incrementAndGet();
            logger.info("created instance " + this);
            
        }

        @PostConstruct
        public void init() {
            logger.info("@PostConstruct called " + this);
            this.postConstructed = true;
        }

        @PreDestroy
        public void destroy() {
            logger.info("@PreDestroy called " + this);
            this.preDestroyed = true;
            instanceCounter.decrementAndGet();
        }

        public boolean isPostConstructed() {
            return postConstructed;
        }

        public boolean isPreDestroyed() {
            return preDestroyed;
        }

        public String getName() {
            return name;
        }

        public static int getInstanceCount() {
            return instanceCounter.get();
        }

        public String toString() {
            return "LifecycleSubject@" + System.identityHashCode(this) + '[' + name + ']';
        }
    }

    private com.netflix.governator.lifecycle.LifecycleManager legacyLifecycleManager;

    private Injector injector;

    private LocalScope localScope;

    @Before
    public void init() throws Exception {
        LifecycleSubject.instanceCounter.set(0);
        localScope = new LocalScope();
        LifecycleInjector lifecycleInjector = LifecycleInjector.builder()
                // .withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS)
                .withAdditionalModules(new ScopingModule()).build();
        injector = lifecycleInjector.createInjector();
        legacyLifecycleManager = lifecycleInjector.getLifecycleManager();

        injector.getInstance(LifecycleManager.class);
        legacyLifecycleManager.start();
        org.apache.log4j.Logger.getLogger("com.netflix.governator").setLevel(Level.WARN);
    }
    
    @Test
    public void testInParallel() throws Exception {
        int concurrency = 1200;
        ExecutorService es = Executors.newFixedThreadPool(concurrency);
        final Random r = new Random(System.currentTimeMillis());
        long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
        System.out.println("totalMemory " + initialMemory);
        final AtomicBoolean running = new AtomicBoolean(true);
        try {
            for (int i=0; i < concurrency; i++) {
                es.submit(new Runnable() {
                   
                    @Override
                    public void run() {
                        while (running.get()) {
                            try {
                                allocateScopedInstance(r.nextInt(500));
                            } catch (InterruptedException e) {
                                // TODO Auto-generated catch block
                                e.printStackTrace();
                            }
                        }
                        
                    }                
                    
                });
            }
                
            es.awaitTermination(60, TimeUnit.SECONDS);
            running.set(false);
            Thread.sleep(500);
        }
        finally {
            es.shutdown();
        }
        legacyLifecycleManager.close();        
        es = null;
        System.gc();
        Thread.sleep(1000);
        System.out.flush();
        System.out.println("total memory: " + initialMemory + "->" + (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()));
        System.out.println("instances count: " + LifecycleSubject.getInstanceCount());
        Assert.assertEquals("instances not predestroyed", 0, LifecycleSubject.getInstanceCount());
        Thread.yield();
    }


    public void allocateScopedInstance(long sleepTime) throws InterruptedException {
        localScope.enter();
        try {
            injector.getInstance(Key.get(LifecycleSubject.class, Names.named("thing1")));
            LifecycleSubject thing1 = injector.getInstance(Key.get(LifecycleSubject.class, Names.named("thing1")));
            Thread.sleep(sleepTime);
            Assert.assertTrue(thing1.isPostConstructed());
            Assert.assertFalse(thing1.isPreDestroyed());
        }
        finally {
            localScope.exit();
        }
    }
}
