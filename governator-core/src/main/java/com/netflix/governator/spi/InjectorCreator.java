package com.netflix.governator.spi;

import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Stage;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjectorCreator;
import com.netflix.governator.SimpleInjectorCreator;

/**
 * Contract that makes Guice injector creation a pluggable strategy and allows for typed
 * extensions to the Injector within the context of the strategy.  An InjectorCreator
 * may also implement post injector creation operations such as calling {@link LifecycleListener}s
 * prior to returning form createInjector().
 * 
 * InjectorCreator can be used directly with a module,
 * 
 * <code>
   new LifecycleInjectorCreator().createInjector(new MyApplicationModule());
 * </code>
 * 
 * Alternatively, InjectorCreator can be used in conjunction with the {@link InjectorBuilder} DSL 
 * 
 * <code>
  LifecycleInjector injector = InjectorBuilder
      .fromModule(new MyApplicationModule())
      .overrideWith(new MyApplicationOverrideModule())
      .combineWith(new AdditionalModule()
      .createInjector(new LifecycleInjectorCreator());
  }
 * </code>
 * 
 * See {@link SimpleInjectorCreator} or {@link LifecycleInjectorCreator}
 */
public interface InjectorCreator<I extends Injector> {
    I createInjector(Stage stage, Module module);
}
