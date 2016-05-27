package com.netflix.governator;

import com.google.inject.AbstractModule;
import com.google.inject.Module;

public class TestSupport {
    private static final class InstancesModule extends AbstractModule {
        final Object[] instances;

        public InstancesModule(Object... instances) {
            this.instances = instances;
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        protected void configure() {
            for (Object o : instances) {
                bind((Class) o.getClass()).toInstance(o);
            }
        }
    }

    public static Module asModule(final Object o) {
        return asModule(o);
    }

    public static Module asModule(final Object... instances) {
        return new InstancesModule(instances);
    }

    public static LifecycleInjector inject(final Object... instances) {
        return InjectorBuilder.fromModule(new InstancesModule()).createInjector();
    }

}
