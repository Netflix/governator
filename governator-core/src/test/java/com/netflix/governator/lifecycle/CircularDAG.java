package com.netflix.governator.lifecycle;

import com.google.inject.Injector;
import com.netflix.governator.LifecycleInjectorBuilderProvider;
import com.netflix.governator.annotations.WarmUp;
import com.netflix.governator.guice.LifecycleInjectorBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * There is a infinite recursion in InternalLifecycleModule.warmUpIsInDag(InternalLifecycleModule.java:150)
 * and InternalLifecycleModule.warmUpIsInDag(InternalLifecycleModule.java:171) that will ultimately lead to
 * an StackOverflowError.
 */
public class CircularDAG extends LifecycleInjectorBuilderProvider
{

    @Singleton
    public static class A
    {

        @Inject
        private B b;
    }

    @Singleton
    public static class B
    {

        @Inject
        private A a;
    }

    @Singleton
    public static class Service
    {

        private final Logger log = LoggerFactory.getLogger(getClass());

        @Inject
        private A a;

        @WarmUp
        public void connect()
        {
            log.info("connect");
        }

        @PreDestroy
        public void disconnect()
        {
            log.info("disconnect");
        }
    }

    @Test(dataProvider = "builders")
    public void circle(LifecycleInjectorBuilder lifecycleInjectorBuilder) throws Exception
    {
        Injector injector = lifecycleInjectorBuilder.createInjector();

        injector.getInstance(Service.class);
        LifecycleManager manager = injector.getInstance(LifecycleManager.class);
        manager.start();
    }
}
