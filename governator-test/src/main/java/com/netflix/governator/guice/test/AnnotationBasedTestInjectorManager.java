package com.netflix.governator.guice.test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.ClassUtils;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Provides;
import com.google.inject.Stage;
import com.google.inject.TypeLiteral;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.netflix.archaius.api.config.CompositeConfig;
import com.netflix.archaius.api.config.SettableConfig;
import com.netflix.archaius.api.inject.RuntimeLayer;
import com.netflix.archaius.config.DefaultSettableConfig;
import com.netflix.archaius.guice.Raw;
import com.netflix.archaius.test.TestCompositeConfig;
import com.netflix.archaius.test.TestPropertyOverride;
import com.netflix.archaius.test.TestPropertyOverrideAnnotationReader;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjector;
import com.netflix.governator.guice.test.mocks.MockHandler;
import com.netflix.governator.providers.SingletonProvider;

public class AnnotationBasedTestInjectorManager {

    private LifecycleInjector injector;
    private final InjectorCreationMode injectorCreationMode;
    private final MockHandler mockHandler;
    private final List<Object> mocksToReset = new ArrayList<>();
    private final List<Module> modulesForTestClass = new ArrayList<>();
    private final List<Module> overrideModules = new ArrayList<>();
    private final List<Key<?>> spyTargets = new ArrayList<>();
    private final SettableConfig classLevelOverrides = new DefaultSettableConfig();
    private final SettableConfig methodLevelOverrides = new DefaultSettableConfig();
    private final TestPropertyOverrideAnnotationReader testPropertyOverrideAnnotationReader = new TestPropertyOverrideAnnotationReader();
    private TestCompositeConfig testCompositeConfig;

    public AnnotationBasedTestInjectorManager(Class<?> classUnderTest, Class<? extends MockHandler> defaultMockHandlerClass) {
        this.injectorCreationMode = getInjectorCreationModeForAnnotatedClass(classUnderTest);
        this.mockHandler = createMockHandlerForTestClass(classUnderTest, defaultMockHandlerClass);
        inspectModulesForTestClass(classUnderTest);
        inspectMocksForTestClass(classUnderTest);
        inspectSpiesForTargetKeys(Elements.getElements(Stage.TOOL, modulesForTestClass));
        overrideModules.add(new ArchaiusTestConfigOverrideModule(classLevelOverrides, methodLevelOverrides));
    }

    public void createInjector() {
        this.injector = createInjector(modulesForTestClass, overrideModules);
        this.testCompositeConfig = getInjector().getInstance(TestCompositeConfig.class);
    }

    /**
     * Injects dependencies into the provided test object.
     */
    public void prepareTestFixture(Object testFixture) {
        getInjector().injectMembers(testFixture);
    }

    public void cleanUpMocks() {
        for (Object mock : mocksToReset) {
            getMockHandler().resetMock(mock);
        }
    }

    public void cleanUpMethodLevelConfig() {
        testCompositeConfig.resetForTest();
    }

    public void cleanUpInjector() {
        getInjector().close();
    }

    public InjectorCreationMode getInjectorCreationMode() {
        return this.injectorCreationMode;
    }

    protected LifecycleInjector createInjector(List<Module> modules, List<Module> overrides) {
        return InjectorBuilder.fromModules(modules).overrideWith(overrides).createInjector();
    }
    
    protected MockHandler createMockHandlerForTestClass(Class<?> testClass, Class<? extends MockHandler> defaultMockHandler) {
        Class<? extends MockHandler> mockHandlerClass = defaultMockHandler;
        for(Class<?> superClass : getAllSuperClassesInReverseOrder(testClass)) {
            ModulesForTesting annotation = superClass.getAnnotation(ModulesForTesting.class);
            if(annotation != null && !annotation.mockHandler().equals(MockHandler.class)) {
                mockHandlerClass = annotation.mockHandler();
            }
        }
        
        ModulesForTesting annotation = testClass.getAnnotation(ModulesForTesting.class);
        if(annotation != null && !annotation.mockHandler().equals(MockHandler.class)) {
            mockHandlerClass = annotation.mockHandler();
        }
        
        if (mockHandlerClass != null) {
            try {
                return mockHandlerClass != MockHandler.class ? mockHandlerClass.newInstance() : defaultMockHandler.newInstance();
            } catch (SecurityException | InstantiationException | IllegalAccessException | IllegalArgumentException e) {
               throw new IllegalArgumentException("Failed to instantiate MockHandler " + mockHandlerClass.getName() + ". Ensure that is has an accessible no-arg constructor.", e);
            }
        } else {
            throw new IllegalStateException("No MockHandler specified!");
        }
    }

    private void inspectModulesForTestClass(Class<?> testClass) {
        final List<Class<? extends Module>> moduleClasses = new ArrayList<>();

        moduleClasses.addAll(getModulesForAnnotatedClass(testClass));
        for (Class<?> parentClass : getAllSuperClassesInReverseOrder(testClass)) {
            moduleClasses.addAll(getModulesForAnnotatedClass(parentClass));
        }
        for (Class<? extends Module> moduleClass : moduleClasses) {
            try {
                modulesForTestClass.add(moduleClass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                try {
                    Constructor<?> zeroArgConstructor = moduleClass.getDeclaredConstructor();
                    zeroArgConstructor.setAccessible(true);
                    modulesForTestClass.add((Module) zeroArgConstructor.newInstance());
                } catch (Exception ex) {
                    throw new RuntimeException("Error instantiating module " + moduleClass
                            + ". Please ensure that the module is public and has a no-arg constructor", e);
                }
            }
        }
    }

    private InjectorCreationMode getInjectorCreationModeForAnnotatedClass(Class<?> testClass) {
        final Annotation annotation = testClass.getAnnotation(ModulesForTesting.class);
        if (annotation != null) {
            return ((ModulesForTesting) annotation).injectorCreation();
        } else {
            return InjectorCreationMode.BEFORE_TEST_CLASS;
        }
    }
    
    private List<Class<? extends Module>> getModulesForAnnotatedClass(Class<?> testClass) {
        final Annotation annotation = testClass.getAnnotation(ModulesForTesting.class);
        if (annotation != null) {
            return Arrays.asList(((ModulesForTesting) annotation).value());
        } else {
            return Collections.emptyList();
        }
    }

    private void inspectMocksForTestClass(Class<?> testClass) {
        getMocksForAnnotatedFields(testClass);
        for (Class<?> parentClass : getAllSuperClassesInReverseOrder(testClass)) {
            getMocksForAnnotatedFields(parentClass);
        }
    }

    private void getMocksForAnnotatedFields(Class<?> testClass) {

        for (Field field : testClass.getDeclaredFields()) {
            if (field.isAnnotationPresent(ReplaceWithMock.class)) {
                overrideModules.add(new MockitoOverrideModule<>(field.getAnnotation(ReplaceWithMock.class), field.getType()));
            }
            if (field.isAnnotationPresent(WrapWithSpy.class)) {
                WrapWithSpy spyAnnotation = field.getAnnotation(WrapWithSpy.class);
                if (spyAnnotation.name().isEmpty()) {
                    spyTargets.add(Key.get(field.getType()));
                } else {
                    spyTargets.add(Key.get(field.getType(), Names.named(spyAnnotation.name())));
                }
            }
        }
    }

    private void inspectSpiesForTargetKeys(List<Element> elements) {
        for (Element element : elements) {
            element.acceptVisitor(new DefaultElementVisitor<Void>() {
                @Override
                public <T> Void visit(Binding<T> binding) {
                    final Binding<T> finalBinding = binding;
                    final Key<T> bindingKey = finalBinding.getKey();
                    final TypeLiteral<T> bindingType = bindingKey.getTypeLiteral();
                    if (spyTargets.contains(binding.getKey())) {
                        AbstractModule spyModule = new AbstractModule() {
                            protected void configure() {
                                final String finalBindingName = "Spied "
                                        + (bindingKey.getAnnotation() != null ? bindingKey.getAnnotation().toString() : "")
                                        + bindingType;
                                final Key<T> newUniqueKey = Key.get(bindingType, Names.named(finalBindingName));
                                finalBinding.acceptTargetVisitor(new CopyBindingTargetVisitor<>(binder().bind(newUniqueKey)));
                                bind(finalBinding.getKey()).toProvider(new SingletonProvider<T>() {
                                    @Inject
                                    Injector injector;

                                    @Override
                                    protected T create() {
                                        T t = injector.getInstance(newUniqueKey);
                                        T spied = getMockHandler().createSpy(t);
                                        mocksToReset.add(spied);
                                        return spied;
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

    public void prepareConfigForTestClass(Class<?> testClass) {
        for (Class<?> parentClass : getAllSuperClassesInReverseOrder(testClass)) {
            classLevelOverrides.setProperties(testPropertyOverrideAnnotationReader.getPropertiesForAnnotation(parentClass.getAnnotation(TestPropertyOverride.class)));
        }
        classLevelOverrides.setProperties(testPropertyOverrideAnnotationReader.getPropertiesForAnnotation(testClass.getAnnotation(TestPropertyOverride.class)));
    }

    public void prepareConfigForTestClass(Class<?> testClass, Method testMethod) {
        prepareConfigForTestClass(testClass);
        methodLevelOverrides.setProperties(testPropertyOverrideAnnotationReader.getPropertiesForAnnotation(testMethod.getAnnotation(TestPropertyOverride.class)));
    }

    private List<Class<?>> getAllSuperClassesInReverseOrder(Class<?> clazz) {
        List<Class<?>> allSuperclasses = ClassUtils.getAllSuperclasses(clazz);
        Collections.reverse(allSuperclasses);
        return allSuperclasses;
    }

    public MockHandler getMockHandler() {
        return mockHandler;
    }

    public LifecycleInjector getInjector() {
        return injector;
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
            final T mock = getMockHandler().createMock(classToBind, annotation.answer().get());
            mocksToReset.add(mock);
            LinkedBindingBuilder<T> bindingBuilder = bind(classToBind);
            if (!annotation.name().isEmpty()) {
                bindingBuilder = ((AnnotatedBindingBuilder<T>) bindingBuilder).annotatedWith(Names.named(annotation.name()));
            }
            bindingBuilder.toInstance(mock);
        }
    }

    private static class ArchaiusTestConfigOverrideModule extends AbstractModule {

        private SettableConfig classLevelOverrides;
        private SettableConfig methodLevelOverrides;

        public ArchaiusTestConfigOverrideModule(SettableConfig classLevelOverrides, SettableConfig methodLevelOverrides) {
            this.classLevelOverrides = classLevelOverrides;
            this.methodLevelOverrides = methodLevelOverrides;
        }

        @Singleton
        private static class OptionalConfigHolder {

            @com.google.inject.Inject(optional = true)
            @RuntimeLayer
            private SettableConfig runtime;

            public SettableConfig get() {
                if (runtime != null) {
                    return runtime;
                } else {
                    return new DefaultSettableConfig();
                }
            }
        }

        @Override
        protected void configure() {
        }

        @Provides
        @Singleton
        public TestCompositeConfig compositeConfig(OptionalConfigHolder config) {
            return new TestCompositeConfig(config.get(), classLevelOverrides, methodLevelOverrides);
        }

        @Provides
        @Singleton
        @Raw
        public CompositeConfig compositeConfig(TestCompositeConfig config) {
            return config;
        }
    }
}
