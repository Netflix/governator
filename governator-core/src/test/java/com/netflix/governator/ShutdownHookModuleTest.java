package com.netflix.governator;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.inject.Singleton;

import org.testng.annotations.Test;

public class ShutdownHookModuleTest {
    @Singleton
    private static class MySingleton {
        @Override
        public String toString() {
            return "MySingleton [initCounter=" + initCounter.get()
                    + ", shutdownCounter=" + shutdownCounter.get() + "]";
        }

        private AtomicInteger initCounter = new AtomicInteger(0);
        private AtomicInteger shutdownCounter = new AtomicInteger(0);
        
        @PostConstruct
        void init() {
            initCounter.incrementAndGet();
        }
        
        @PreDestroy
        void shutdown() {
            shutdownCounter.incrementAndGet();
        }
    }
    
    @Test
    public void test() throws InterruptedException {
        System.out.println("Starting");
        LifecycleInjector injector = Governator.createInjector(new LifecycleModule(), new ShutdownHookModule());
        Executors.newScheduledThreadPool(1).schedule(new Runnable(){
            @Override
            public void run() {
                System.out.println("Terminating");
                System.exit(-1);
            }
        }, 100, TimeUnit.MILLISECONDS);
        
        MySingleton singleton = injector.getInstance(MySingleton.class);
        
        System.out.println("Waiting");
        injector.awaitTermination();
        System.out.println("Terminated : " + singleton);
    }
}
