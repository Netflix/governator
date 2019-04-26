package com.netflix.governator.guice;

import com.google.inject.AbstractModule;
import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import com.google.inject.Singleton;
import com.google.inject.multibindings.OptionalBinder;
import com.netflix.governator.lifecycle.LifecycleManager;
import org.junit.Assert;
import org.junit.Test;

import java.util.Optional;

import javax.annotation.PreDestroy;
import javax.inject.Provider;

public class TestLifecycleInjector {

  public <T extends PreDestroyOnce> void checkCleanup(Module m, Class<T> type) throws Exception {
    /*
     * If the PreDestroy method for the object is called more than once, then it
     * means there is a memory leak because we have registered multiple
     * ManagedInstanceActions in the PreDestroyMonitor
     */
    Injector injector = LifecycleInjector.builder().withModules(m).build().createInjector();

    LifecycleManager mgr = injector.getInstance(LifecycleManager.class);
    mgr.start();

    Object[] objs = new Object[25];
    for (int i = 0; i < 25; ++i) {
      objs[i] = injector.getInstance(type);
    }

    mgr.close();

    for (Object o : objs) {
      T t = (T) o;
      Assert.assertTrue(t.isClosed());
      int count = t.getPreDestroyCallCount();
      Assert.assertEquals("count equals " + count, 1, count);
    }

  }

  /** registers ProviderInstanceBinding with Singleton scope 1 time */
  @Test
  public void providedInstanceBindingPreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {
      }

      @Singleton
      @Provides
      public PreDestroyOnce providesObj(String param1) {
        return new PreDestroyOnceImpl();
      }
    }, PreDestroyOnce.class);
  }

  /** registers ProviderInstanceBinding with 'no scope' 25 times */
  @Test
  public void providedInstanceBindingUnscopedPreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {
      }

      @Provides
      public PreDestroyOnce providesObj(String name) {
        return new PreDestroyOnceImpl();
      }
    }, PreDestroyOnce.class);
  }

  /**
   * registers ConstructorBindingImpl with Singleton scope 1 time
   */
  @Test
  public void optionalBindingPreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {
        OptionalBinder.newOptionalBinder(binder(), PreDestroyOnce.class).setDefault().to(PreDestroyOnceImpl.class)
            .in(Scopes.SINGLETON);
      }
    }, PreDestroyOnce.class);
  }

  /**
   * registers ConstructorBindingImpl with Singleton scope 1 time
   */
  @Test
  public void optionalBindingUnscopedPreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {
        OptionalBinder.newOptionalBinder(binder(), PreDestroyOnce.class).setDefault().to(PreDestroyOnceImpl.class);
      }
    }, PreDestroyOnce.class);
  }

  /**
   * registers LinkedProviderBinding in Singleton scope 1 time
   */
  @Test
  public void optionalProviderBindingPreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {
        OptionalBinder.newOptionalBinder(binder(), PreDestroyOnce.class).setDefault()
            .toProvider(PreDestroyOnceProvider.class).in(Scopes.SINGLETON);
      }
    }, PreDestroyOnce.class);
  }

  /**
   * registers InstanceBinding in Singleton scope 1 time
   */
  @Test
  public void optionalProviderBindingUnscopedPreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {
        OptionalBinder.newOptionalBinder(binder(), PreDestroyOnce.class).setDefault()
            .toProvider(PreDestroyOnceProvider.class);

        OptionalBinder.newOptionalBinder(binder(), PreDestroyOnce.class).setBinding()
            .toInstance(new PreDestroyOnceImpl());

      }
    }, PreDestroyOnce.class);
  }

/**
 * registers InstanceBinding in Singleton scope 1 time
 */
@Test
  public void providedInstancePreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {
        OptionalBinder.newOptionalBinder(binder(), PreDestroyOnce.class).setDefault()
            .toInstance(new PreDestroyOnceImpl());
      }
    }, PreDestroyOnce.class);
  }

  /**
   * registers ConstructorBinding in Singleton scope 1 time
   */
  @Test
  public void testAutomaticBindingPreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {
      }
    }, PreDestroyOnceImpl.class);
  }

  /**
   * registers ConstructorBinding in 'No Scope' 25 times
   * @throws Exception
   */
  @Test
  public void testAutomaticBindingUnboundPreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {
      }
    }, PreDestroyOnceProtoImpl.class);
  }

  /**
   * registers ProviderMethodProviderInstanceBinding in Singleton scope 1 times
   */
  @Test
  public void testSingletonProviderPreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {

      }

      @Provides
      @Singleton
      public PreDestroyOnce providesObj(String name) {
        return new PreDestroyOnceImpl();
      }
    }, JavaxSingletonConsumer.class);
  }

  /**
   * registers ProviderMethodProviderInstanceBinding in 'No Scope' 25 times
   */
  @Test
  public void testUnscopedProviderPreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {

      }

      @Provides
      public PreDestroyOnce providesObj(String name) {
        return new PreDestroyOnceImpl();
      }
    }, JavaxSingletonConsumer.class);
  }

 /**
   * registers LinkedProviderBinding in 'No Scope' 25 times
   */
   @Test
  public void testOptionalPreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {
        OptionalBinder.newOptionalBinder(binder(), PreDestroyOnce.class).setDefault()
            .toProvider(PreDestroyOnceProvider.class);
      }

    }, JavaxOptionalConsumer.class);
  }

  /**
   * registers LinkedProviderBinding in Singleton scope 1 time
   */
  @Test
  public void testSingletonOptionalPreDestroy() throws Exception {
    checkCleanup(new AbstractModule() {
      @Override
      protected void configure() {
        OptionalBinder.newOptionalBinder(binder(), PreDestroyOnce.class).setDefault()
            .toProvider(PreDestroyOnceProvider.class).asEagerSingleton();
      }

    }, JavaxOptionalConsumer.class);
  }


  private interface PreDestroyOnce {
    boolean isClosed();

    int getPreDestroyCallCount();
  }

  private static class JavaxSingletonConsumer implements PreDestroyOnce {
    private final PreDestroyOnce delegate;

    @Inject
    JavaxSingletonConsumer(Provider<PreDestroyOnce> provider) {
      this.delegate = provider.get();
    }

    public boolean isClosed() {
      return delegate.isClosed();
    }

    public int getPreDestroyCallCount() {
      return delegate.getPreDestroyCallCount();
    }
  }

  private static class JavaxOptionalConsumer implements PreDestroyOnce {
    private final PreDestroyOnce delegate;

    @Inject
    JavaxOptionalConsumer(Optional<PreDestroyOnce> provider) {
      this.delegate = provider.orElse(null);
    }

    public boolean isClosed() {
      return delegate.isClosed();
    }

    public int getPreDestroyCallCount() {
      return delegate.getPreDestroyCallCount();
    }
  }

  @Singleton
  private static class PreDestroyOnceImpl implements PreDestroyOnce {

    private boolean closed = false;
    private int counter = 0;

    @Override
    public boolean isClosed() {
      return closed;
    }

    @PreDestroy
    public void close() {
      ++counter;
      closed = true;
    }

    @Override
    public int getPreDestroyCallCount() {
      return counter;
    }
  }

  private static class PreDestroyOnceProtoImpl implements PreDestroyOnce {

    private boolean closed = false;
    private int counter = 0;

    @Override
    public boolean isClosed() {
      return closed;
    }

    @PreDestroy
    public void close() {
      ++counter;
      closed = true;
    }

    @Override
    public int getPreDestroyCallCount() {
      return counter;
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
