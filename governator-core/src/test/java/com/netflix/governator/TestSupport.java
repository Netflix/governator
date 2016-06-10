package com.netflix.governator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.IdentityHashMap;
import java.util.List;

import com.google.inject.AbstractModule;
import com.google.inject.Module;
import com.google.inject.Stage;

public class TestSupport {
    private static final class InstancesModule extends AbstractModule {
        final List<Object> instances;

        public InstancesModule(Object... instances) {
            this.instances = new ArrayList<>(Arrays.asList(instances));
        }

        @SuppressWarnings({ "unchecked", "rawtypes" })
        @Override
        protected void configure() {
            for (Object o : instances) {
                Class clz = (Class) o.getClass();
                bind(clz).toInstance(o);
            }
        }
    }

    private InstancesModule module = new InstancesModule();
    private IdentityHashMap<GovernatorFeature<?>, Object> features = new IdentityHashMap<>();
    
    
    public static Module asModule(final Object o) {
        return asModule(o);
    }

    public static Module asModule(final Object... instances) {
        return new InstancesModule(instances);
    }

    public static LifecycleInjector inject(final Object... instances) {
        return InjectorBuilder.fromModule(new InstancesModule(instances)).createInjector();
    }
    
    public <T> TestSupport withFeature(GovernatorFeature<T> feature, T value) {
        this.features.put(feature, value);
        return this;
    }
    
    public TestSupport withSingleton(final Object... instances) {
        module.instances.addAll(Arrays.asList(instances));
        return this;       
    }
    
    public LifecycleInjector inject() {
        return new LifecycleInjectorCreator().withFeatures(features).createInjector(Stage.PRODUCTION, module);
    }

}
