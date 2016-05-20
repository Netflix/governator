package com.netflix.governator.guice.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.ClassUtils;
import org.mockito.Mockito;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjector;
import com.netflix.governator.providers.SingletonProvider;

public class AnnotationBasedTestInjectorCreator {

    private final LifecycleInjector injector;
    private final List<Object> mocksToReset = new ArrayList<>();
    private final List<Module> modulesForTestClass = new ArrayList<>();
    private final List<Module> overrideModules = new ArrayList<>();
    private final List<Key<?>> spyTargets = new ArrayList<>();

    public AnnotationBasedTestInjectorCreator(Class<?> classUnderTest) {
        getModulesForTestClass(classUnderTest);
        getMocksForTestClass(classUnderTest);
        getSpiesForTargetKeys(Elements.getElements(modulesForTestClass));
        injector = createInjector(modulesForTestClass, overrideModules);
    }

    /**
     * Injects dependencies into the provided test object.
     */
    public void prepareTestFixture(Object testFixture) {
        injector.injectMembers(testFixture);
    }

    public void cleanupMocks() {
        for (Object mock : mocksToReset) {
            Mockito.reset(mock);
        }
    }

    public void cleanupInjector() {
        injector.shutdown();
    }

    private LifecycleInjector createInjector(List<Module> modules, List<Module> overrides) {
        return InjectorBuilder.fromModules(modules).overrideWith(overrides).createInjector();
    }

    private void getModulesForTestClass(Class<?> testClass) {
        final List<Class<? extends Module>> moduleClasses = new ArrayList<>();

        moduleClasses.addAll(getModulesForAnnotatedClass(testClass));
        for (Class<?> parentClass : ClassUtils.getAllSuperclasses(testClass)) {
            moduleClasses.addAll(getModulesForAnnotatedClass(parentClass));
        }
        for (Class<? extends Module> moduleClass : moduleClasses) {
            try {
                modulesForTestClass.add(moduleClass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                try {
                    Constructor<?> constructor = moduleClass.getDeclaredConstructors()[0];
                    constructor.setAccessible(true);
                    modulesForTestClass.add((Module) constructor.newInstance());
                } catch (Exception ex) {
                    throw new RuntimeException("Error instantiating module " + moduleClass
                            + ". Please ensure that the module is public and has a no-arg constructor", e);
                }
            }
        }
    }

    private List<Class<? extends Module>> getModulesForAnnotatedClass(Class<?> clazz) {
        final Annotation annotation = clazz.getAnnotation(ModulesForTesting.class);
        if (annotation != null) {
            return Arrays.asList(((ModulesForTesting) annotation).value());
        } else {
            return Collections.emptyList();
        }
    }

    private void getMocksForTestClass(Class<?> testClass) {
        getMocksForAnnotatedFields(testClass);
        for (Class<?> parentClass : ClassUtils.getAllSuperclasses(testClass)) {
            getMocksForAnnotatedFields(parentClass);
        }
    }

    @SuppressWarnings("rawtypes")
    private void getMocksForAnnotatedFields(Class<?> clazz) {

        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ReplaceWithMock.class)) {
                overrideModules.add(new MockitoOverrideModule(field.getAnnotation(ReplaceWithMock.class), field.getType()));
            }
            if (field.isAnnotationPresent(WrapWithSpy.class)) {
                spyTargets.add(Key.get(field.getType()));
            }
        }
    }

    private void getSpiesForTargetKeys(List<Element> elements) {
        for (Element element : elements) {
            element.acceptVisitor(new DefaultElementVisitor<Void>() {
                @Override
                public <T> Void visit(Binding<T> binding) {
                    final Binding<T> finalBinding = binding;
                    if (spyTargets.contains(binding.getKey())) {
                        AbstractModule spyModule = new AbstractModule() {
                            protected void configure() {

                                final Key newUniqueKey = Key.get(finalBinding.getKey().getTypeLiteral().getRawType(),
                                        Names.named("Spied " + finalBinding.getKey().getTypeLiteral().getRawType()));
                                finalBinding.acceptTargetVisitor(new CopyBindingTargetVisitor<>(binder().bind(newUniqueKey)));
                                bind(finalBinding.getKey()).toProvider(new SingletonProvider<T>() {
                                    @Inject
                                    Injector injector;

                                    protected T create() {
                                        T t = (T) injector.getInstance(newUniqueKey);
                                        return Mockito.spy(t);
                                    };
                                });
                            }
                        };
                        overrideModules.add(spyModule);
                    }
                    return null;
                }
            });
        }
    }

    private class MockitoOverrideModule<T> extends AbstractModule {
        private final ReplaceWithMock annotation;
        private final Class<T> classToBind;

        public MockitoOverrideModule(ReplaceWithMock annotation, Class<T> classToBind) {
            this.annotation = annotation;
            this.classToBind = classToBind;
        }

        @Override
        protected void configure() {
            final T mock = Mockito.mock(classToBind, annotation.answer().get());
            mocksToReset.add(mock);
            LinkedBindingBuilder<T> bindingBuilder = bind(classToBind);
            if (!annotation.name().isEmpty()) {
                bindingBuilder = ((AnnotatedBindingBuilder<T>) bindingBuilder).annotatedWith(Names.named(annotation.name()));
            }
            bindingBuilder.toInstance(mock);
        }
    }
}
