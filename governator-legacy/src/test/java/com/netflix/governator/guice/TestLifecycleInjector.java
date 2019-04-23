package com.netflix.governator.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.OptionalBinder;
import com.netflix.governator.lifecycle.LifecycleManager;
import org.junit.Assert;
import org.junit.Test;

import javax.annotation.PreDestroy;
import javax.inject.Provider;

public class TestLifecycleInjector {

  public void checkCleanup(Module m) throws Exception {
    // If the PreDestroy method for the object is called more than once, then it means
    // there is a memory leak because we have registered multiple ManagedInstanceActions
    // in the PreDestroyMonitor
    Injector injector = LifecycleInjector.builder()
        .withModules(m)
        .build()
        .createInjector();

    LifecycleManager mgr = injector.getInstance(LifecycleManager.class);
    mgr.start();

    PreDestroyOnce obj = injector.getInstance(PreDestroyOnce.class);
    for (int i = 0; i < 1000; ++i) {
      PreDestroyOnce tmp = injector.getInstance(PreDestroyOnce.class);
      Assert.assertSame(obj, tmp);
    }

    mgr.close();

    Assert.assertTrue(obj.isClosed());
  }

  @Test
  public void providedBindingPreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {
      }

      @Singleton
      @Provides
      public PreDestroyOnce providesObj() {
        return new PreDestroyOnceImpl();
      }
    });
  }

  @Test
  public void optionalBindingPreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {
        OptionalBinder.newOptionalBinder(binder(), PreDestroyOnce.class)
            .setDefault()
            .to(PreDestroyOnceImpl.class)
            .in(Scopes.SINGLETON);
      }
    });
  }

  @Test
  public void optionalProviderBindingPreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {
        OptionalBinder.newOptionalBinder(binder(), PreDestroyOnce.class)
            .setDefault()
            .toProvider(PreDestroyOnceProvider.class)
            .in(Scopes.SINGLETON);
      }
    });
  }

  private interface PreDestroyOnce {
    boolean isClosed();
  }

  @Singleton
  private static class PreDestroyOnceImpl implements PreDestroyOnce {

    private boolean closed = false;

    @Override public boolean isClosed() {
      return closed;
    }

    @PreDestroy
    public void close() {
      Assert.assertFalse(closed);
      closed = true;
    }
  }

  @Singleton
  private static class PreDestroyOnceProvider implements Provider<PreDestroyOnce> {

    @Override
    public PreDestroyOnce get() {
      return new PreDestroyOnceImpl();
    }
  }
}
