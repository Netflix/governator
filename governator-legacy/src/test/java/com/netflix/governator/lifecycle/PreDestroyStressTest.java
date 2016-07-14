package com.netflix.governator.lifecycle;

import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;
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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.google.inject.name.Names;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import com.netflix.governator.guice.LifecycleInjectorMode;
import com.tngtech.java.junit.dataprovider.DataProvider;
import com.tngtech.java.junit.dataprovider.DataProviderRunner;
import com.tngtech.java.junit.dataprovider.UseDataProvider;

@RunWith(DataProviderRunner.class)
public class PreDestroyStressTest {
    static final Key<LifecycleSubject> LIFECYCLE_SUBJECT_KEY = Key.get(LifecycleSubject.class);
    private static final int TEST_TIME_IN_SECONDS = 10;  // run the stress test for this many seconds
    private static final int CONCURRENCY_LEVEL = 20; // run the stress test with this many threads

    private final class ScopingModule extends AbstractModule {
        LocalScope localScope = new LocalScope();
        @Override
        protected void configure() {
            bindScope(LocalScoped.class, localScope);
            bind(LifecycleSubject.class).in(localScope);
        }

        @Provides
        @Singleton
        @Named("thing1")
        public LifecycleSubject thing1() {
            return new LifecycleSubject("thing1");
        }
    }


    static class LifecycleSubject {
        private static AtomicInteger instanceCounter = new AtomicInteger(0);
        private static AtomicInteger preDestroyCounter = new AtomicInteger(0);
        private static AtomicInteger postConstructCounter = new AtomicInteger(0);
        private static Logger logger = LoggerFactory.getLogger(LifecycleSubject.class);
        private static byte[] bulkTemplate;
        static {
            bulkTemplate = new byte[1024*100]; 
            Arrays.fill(bulkTemplate, (byte)0);        
        }

        private String name;
        private volatile boolean postConstructed = false;
        private volatile boolean preDestroyed = false;
        private byte[] bulk;
        private final String toString;

        public LifecycleSubject() {
            this("anonymous");
        }
        
        public LifecycleSubject(String name) {
            this.name = name;
            this.bulk = bulkTemplate.clone(); 
            int instanceId = instanceCounter.incrementAndGet();
            this.toString = "LifecycleSubject@" + instanceId + '[' + name + ']';
            logger.info("created instance {} {}", toString, instanceId);
            
        }
        
        public static void clear() {
            instanceCounter.set(0);
            postConstructCounter.set(0);
            preDestroyCounter.set(0);
        }

        @PostConstruct
        public void init() {
            logger.info("@PostConstruct called {} {}", toString, postConstructCounter.incrementAndGet());
            this.postConstructed = true;
        }

        @PreDestroy
        public void destroy() {
            logger.info("@PreDestroy called {} {}", toString, preDestroyCounter.incrementAndGet());
            this.preDestroyed = true;
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

        public String toString() {
            return toString;
        }
    }

    @Test
    @UseDataProvider("builders")
    public void testInParallel(String name, LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception {
        LifecycleSubject.clear();
        ScopingModule scopingModule = new ScopingModule();
        final LocalScope localScope = scopingModule.localScope;
        LifecycleInjector lifecycleInjector = LifecycleInjector.builder()
                .withAdditionalModules(scopingModule).build();
        SecureRandom random = new SecureRandom();
        LifecycleSubject thing1 = null;
        try (com.netflix.governator.lifecycle.LifecycleManager legacyLifecycleManager = lifecycleInjector.getLifecycleManager()) {
            org.apache.log4j.Logger.getLogger("com.netflix.governator").setLevel(Level.WARN);
            final Injector injector = lifecycleInjector.createInjector(); 
            legacyLifecycleManager.start();
                    
            thing1 = injector.getInstance(Key.get(LifecycleSubject.class, Names.named("thing1")));
            Assert.assertEquals("singleton instance not postConstructed", LifecycleSubject.postConstructCounter.get(), LifecycleSubject.instanceCounter.get());
            Assert.assertEquals("singleton instance predestroyed too soon", LifecycleSubject.preDestroyCounter.get(), LifecycleSubject.instanceCounter.get()-1);
            
            Callable<Void> scopingTask = allocateScopedInstance(injector, localScope, random);
            runInParallel(CONCURRENCY_LEVEL, scopingTask, TEST_TIME_IN_SECONDS, TimeUnit.SECONDS);
            
            System.gc();
            Thread.sleep(1000);
            System.out.println("instances count: " + LifecycleSubject.instanceCounter.get());
            System.out.flush();
            Assert.assertEquals("instances not postConstructed", LifecycleSubject.postConstructCounter.get(), LifecycleSubject.instanceCounter.get());
            Assert.assertEquals("scoped instances not predestroyed", LifecycleSubject.preDestroyCounter.get(), LifecycleSubject.instanceCounter.get()-1);
            Assert.assertFalse("singleton instance was predestroyed too soon", thing1.isPreDestroyed());
        }
        finally {
            org.apache.log4j.Logger.getLogger("com.netflix.governator").setLevel(Level.DEBUG);            
        }
        Assert.assertTrue("singleton instance was not predestroyed", thing1.isPreDestroyed());
        Thread.yield();
    }


    void runInParallel(int concurrency, final Callable<Void> task, int duration, TimeUnit timeUnits) throws InterruptedException {

        ExecutorService es = Executors.newFixedThreadPool(concurrency, new ThreadFactoryBuilder().setNameFormat("predestroy-stress-test-%d").build());
        final AtomicBoolean running = new AtomicBoolean(true);
        try {
            List<Callable<Void>> tasks = new ArrayList<>();
            for (int i=0; i < concurrency; i++) {
                tasks.add(new Callable<Void>() {                       
                    @Override
                    public Void call() {
                        while (running.get()) {
                            try {
                                task.call();
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                        return null;
                    }                
                    
                });
            }
            es.invokeAll(tasks, duration, timeUnits);
        }
        finally {
            running.set(false);            
            es.shutdown();
            es.awaitTermination(10, TimeUnit.SECONDS);
        }
        es = null;
    }


    Callable<Void> allocateScopedInstance(final Injector injector, final LocalScope localScope, final SecureRandom random) {
        return new Callable<Void>() {
            public Void call() throws InterruptedException {
                localScope.enter();
                try {
                    LifecycleSubject anonymous = injector.getInstance(LIFECYCLE_SUBJECT_KEY);
                    Thread.sleep(random.nextInt(500));
                    Assert.assertTrue(anonymous.isPostConstructed());
                    Assert.assertFalse(anonymous.isPreDestroyed());
                }
                catch(InterruptedException e) {
                    
                }
                finally {
                    localScope.exit();
                }
                return null;
            }
        };

    }
    
    @DataProvider
    public static Object[][] builders()
    {
        return new Object[][]
        {
            new Object[] { "simulatedChildInjector", LifecycleInjector.builder().withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS) },
            new Object[] { "childInjector", LifecycleInjector.builder() }
        };
    }
    
    
}
