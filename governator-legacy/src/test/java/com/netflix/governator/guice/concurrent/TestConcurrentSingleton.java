package com.netflix.governator.guice.concurrent;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import junit.framework.Assert;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.inject.AbstractModule;
import com.google.inject.ImplementedBy;
import com.google.inject.Injector;
import com.netflix.governator.annotations.NonConcurrent;
import com.netflix.governator.guice.BootstrapBinder;
import com.netflix.governator.guice.BootstrapModule;
import com.netflix.governator.guice.LifecycleInjector;
import com.netflix.governator.guice.LifecycleInjectorMode;
import com.netflix.governator.guice.actions.BindingReport;
import com.netflix.governator.guice.lazy.FineGrainedLazySingleton;
import com.netflix.governator.lifecycle.LoggingLifecycleListener;

public class TestConcurrentSingleton {
    private static Logger LOG = LoggerFactory.getLogger(TestConcurrentSingleton.class);
    
    public static interface IParent {
        
    }
    
    @FineGrainedLazySingleton
    public static class Parent implements IParent {
        @Inject
        Parent(@NonConcurrent NonConcurrentSingleton nonConcurrent, 
                SlowChild1 child1, 
                SlowChild2 child2, 
                Provider<SlowChild3> child3, 
                NonSingletonChild child4, 
                NonSingletonChild child5) {
            LOG.info("Parent start");
            LOG.info("Parent end");
        }
        
        @PostConstruct
        public void init() {
            LOG.info("PostConstruct");
        }
    }
    
    @Singleton
    public static class NonConcurrentSingleton {
        @Inject
        public NonConcurrentSingleton(Recorder recorder) throws InterruptedException {
            recorder.record(getClass());
            LOG.info("NonConcurrentSingleton start");
            TimeUnit.SECONDS.sleep(1);
            LOG.info("NonConcurrentSingleton end");
        }
    }
    
    @FineGrainedLazySingleton
    public static class SlowChild1 {
        @Inject
        public SlowChild1(Recorder recorder) throws InterruptedException {
            recorder.record(getClass());
            LOG.info("Child1 start");
            TimeUnit.SECONDS.sleep(1);
            LOG.info("Child1 end");
        }
    }
    
    @FineGrainedLazySingleton
    public static class SlowChild2 {
        @Inject
        public SlowChild2(Recorder recorder) throws InterruptedException {
            recorder.record(getClass());
            LOG.info("Child2 start");
            TimeUnit.SECONDS.sleep(1);
            LOG.info("Child2 end");
        }
    }
    
    @ImplementedBy(SlowChild3Impl.class)
    public static interface SlowChild3 {
        
    }
    
    @FineGrainedLazySingleton
    public static class SlowChild3Impl implements SlowChild3 {
        @Inject
        public SlowChild3Impl(Recorder recorder) throws InterruptedException {
            recorder.record(getClass());
            LOG.info("Child3 start");
            TimeUnit.SECONDS.sleep(1);
            LOG.info("Child3 end");
        }
    }
    
    public static class NonSingletonChild {
        private static AtomicInteger counter = new AtomicInteger();
        
        @Inject
        public NonSingletonChild(Recorder recorder) throws InterruptedException {
            recorder.record(getClass());
            int count = counter.incrementAndGet();
            LOG.info("NonSingletonChild start " + count);
            TimeUnit.SECONDS.sleep(1);
            LOG.info("NonSingletonChild end " + count);
        }
    }
    
    @FineGrainedLazySingleton
    public static class Recorder {
        Map<Class<?>, Integer> counts = Maps.newHashMap();
        Map<Class<?>, Long> threadIds = Maps.newHashMap();
        Set<Long> uniqueThreadIds = Sets.newHashSet();
        
        public synchronized void record(Class<?> type) {
            threadIds.put(type, Thread.currentThread().getId());
            uniqueThreadIds.add(Thread.currentThread().getId());
            if (!counts.containsKey(type)) {
                counts.put(type, 1);
            }
            else {
                counts.put(type, counts.get(type)+1);
            }
        }
        
        public int getUniqueThreadCount() {
            return uniqueThreadIds.size();
        }
        
        public int getTypeCount(Class<?> type) {
            Integer count = counts.get(type);
            return count == null ? 0 : count;
        }
        
        public long getThreadId(Class<?> type) {
            return threadIds.get(type);
        }
    }
    
    @Test
    public void shouldInitInterfaceInParallel() {
        Injector injector = LifecycleInjector.builder()
                .withPostInjectorAction(new BindingReport("Report"))
                .withMode(LifecycleInjectorMode.SIMULATED_CHILD_INJECTORS)
                .withAdditionalBootstrapModules(new BootstrapModule() {
                    @Override
                    public void configure(BootstrapBinder binder) {
                        binder.bindLifecycleListener().to(LoggingLifecycleListener.class);
                    }
                })
                .withModules(new AbstractModule() {
                    @Override
                    protected void configure() {
                        bind(IParent.class).to(Parent.class);
                        bind(Parent.class).toProvider(ConcurrentProviders.of(Parent.class));
                    }
                })
                .build().createInjector();
        
        // This confirms that the Provider is called via the interface binding
        injector.getInstance(IParent.class);
        Recorder recorder = injector.getInstance(Recorder.class);
        
        long getMainThreadId = Thread.currentThread().getId();
        Assert.assertEquals(5, recorder.getUniqueThreadCount());
        Assert.assertEquals(1, recorder.getTypeCount(SlowChild1.class));
        Assert.assertEquals(1, recorder.getTypeCount(SlowChild2.class));
        Assert.assertEquals(0, recorder.getTypeCount(SlowChild3.class));        // Only the provider was injected
        Assert.assertEquals(2, recorder.getTypeCount(NonSingletonChild.class));
        Assert.assertEquals(getMainThreadId, recorder.getThreadId(NonConcurrentSingleton.class));
        
    }
}
