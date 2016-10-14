package com.netflix.governator;

import com.google.inject.AbstractModule;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.ProvisionListener;
import com.netflix.governator.package1.AutoBindSingletonConcrete;
import com.netflix.governator.package1.AutoBindSingletonInterface;
import com.netflix.governator.package1.AutoBindSingletonMultiBinding;
import com.netflix.governator.package1.AutoBindSingletonWithInterface;
import com.netflix.governator.package1.FooModule;

import org.junit.Assert;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

public class AutoBindSingletonTest {
    @Test
    public void confirmBindingsCreatedForAutoBindSinglton() {
        final Set<Class> created = new HashSet<>();
        try (LifecycleInjector injector = InjectorBuilder.fromModules(
            new ScanningModuleBuilder()
                .forPackages("com.netflix.governator.package1", "com.netflix")
                .addScanner(new AutoBindSingletonAnnotatedClassScanner())
                .build(),
            new AbstractModule() {
                @Override
                protected void configure() {
                    bindListener(Matchers.any(), new ProvisionListener() {
                        @Override
                        public <T> void onProvision(ProvisionInvocation<T> provision) {
                            Class<?> type = provision.getBinding().getKey().getTypeLiteral().getRawType();
                            if (type != null && type.getName().startsWith("com.netflix.governator.package1")) {
                                created.add(type);
                            }
                        }
                    });
                }
            })
        .createInjector()) {
            Assert.assertTrue(created.contains(AutoBindSingletonConcrete.class));
            
            injector.getInstance(Key.get(new TypeLiteral<Set<AutoBindSingletonInterface>>() {}));
            injector.getInstance(Key.get(AutoBindSingletonInterface.class));
            
            Assert.assertTrue(created.contains(AutoBindSingletonMultiBinding.class));
            Assert.assertTrue(created.contains(AutoBindSingletonWithInterface.class));
            Assert.assertEquals(injector.getInstance(String.class), "AutoBound");
        }
    }
    
    @Test
    public void confirmNoBindingsForExcludedClass() {
        try (LifecycleInjector injector = InjectorBuilder.fromModules(
            new ScanningModuleBuilder()
                .forPackages("com.netflix.governator.package1")
                .addScanner(new AutoBindSingletonAnnotatedClassScanner())
                .excludeClasses(AutoBindSingletonConcrete.class)
                .build())
        .createInjector()) {
            Assert.assertNull(injector.getExistingBinding(Key.get(AutoBindSingletonConcrete.class)));
        }
    }
    
    @Test
    public void confirmModuleDedupingWorksWithScannedClasse() {
        try (LifecycleInjector injector = InjectorBuilder.fromModules(
            new ScanningModuleBuilder()
                .forPackages("com.netflix.governator.package1")
                .addScanner(new AutoBindSingletonAnnotatedClassScanner())
                .excludeClasses(AutoBindSingletonConcrete.class)
                .build(),
            new FooModule()
            )
        .createInjector()) {
            Assert.assertNull(injector.getExistingBinding(Key.get(AutoBindSingletonConcrete.class)));
        }
    }
}
