package com.netflix.governator.lifecycle;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;
import com.google.inject.Injector;
import com.netflix.governator.annotations.WarmUp;
import com.netflix.governator.guice.LifecycleInjector;

/**
 * There is a infinite recursion in InternalLifecycleModule.warmUpIsInDag(InternalLifecycleModule.java:150)
 * and InternalLifecycleModule.warmUpIsInDag(InternalLifecycleModule.java:171) that will ultimately lead to
 * an StackOverflowError.
 */
public class CircularDAG {

  @Singleton
  public static class A {

    @Inject
    private B b;
  }

  @Singleton
  public static class B {

    @Inject
    private A a;
  }

  @Singleton
  public static class Service {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Inject
    private A a;

    @WarmUp
    public void connect() {
      log.info("connect");
    }

    @PreDestroy
    public void disconnect() {
      log.info("disconnect");
    }
  }

  @Test
  public void circle() throws StackOverflowError, Exception {
    Injector injector = LifecycleInjector.builder().createInjector();

    injector.getInstance(Service.class);
    LifecycleManager manager = injector.getInstance(LifecycleManager.class);
    manager.start();
  }
}
