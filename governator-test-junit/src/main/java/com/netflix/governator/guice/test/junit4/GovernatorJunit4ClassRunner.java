package com.netflix.governator.guice.test.junit4;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.ClassUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.internal.runners.statements.RunAfters;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.mockito.Mockito;

import com.google.inject.AbstractModule;
import com.google.inject.Binding;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Module;
import com.google.inject.Scope;
import com.google.inject.Scopes;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.internal.Scoping;
import com.google.inject.name.Names;
import com.google.inject.spi.BindingScopingVisitor;
import com.google.inject.spi.BindingTargetVisitor;
import com.google.inject.spi.ConstructorBinding;
import com.google.inject.spi.ConvertedConstantBinding;
import com.google.inject.spi.DefaultElementVisitor;
import com.google.inject.spi.Element;
import com.google.inject.spi.Elements;
import com.google.inject.spi.ExposedBinding;
import com.google.inject.spi.InstanceBinding;
import com.google.inject.spi.LinkedKeyBinding;
import com.google.inject.spi.ProviderBinding;
import com.google.inject.spi.ProviderInstanceBinding;
import com.google.inject.spi.ProviderKeyBinding;
import com.google.inject.spi.UntargettedBinding;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjector;
import com.netflix.governator.providers.SingletonProvider;

/**
 * An extended {@link BlockJUnit4ClassRunner} which creates a Governator-Guice
 * injector from a list of modules, as well as provides utilities for
 * Mocking/Spying bindings.
 * 
 * See {@link ModulesForTesting}, {@link ReplaceWithMock}, and
 * {@link WrapWithSpy} for example usage.
 */
public class GovernatorJunit4ClassRunner extends BlockJUnit4ClassRunner {

    private final LifecycleInjector injector;
    private final List<Object> mocksToReset = new ArrayList<>();
    private final List<Module> modulesForTestClass = new ArrayList<>();
    private final List<Module> overrideModules = new ArrayList<>();
    private final List<Key<?>> spyTargets = new ArrayList<>();

    public GovernatorJunit4ClassRunner(Class<?> klass) throws InitializationError {
        super(klass);
        getModulesForTestClass(klass);
        getMocksForTestClass(klass);
        getSpiesForTargetKeys(Elements.getElements(modulesForTestClass));
        injector = createInjector(modulesForTestClass, overrideModules);
    }

    @Override
    protected Object createTest() throws Exception {
        final Object testInstance = super.createTest();
        prepareTestInstance(testInstance);
        return testInstance;
    }

    @Override
    protected Statement withAfters(FrameworkMethod method, Object target, Statement statement) {
        final List<FrameworkMethod> afters = getTestClass().getAnnotatedMethods(After.class);
        return new RunAfters(statement, afters, target) {
            @Override
            public void evaluate() throws Throwable {
                super.evaluate();
                cleanupMocks();
            }
        };
    }

    @Override
    protected Statement withAfterClasses(Statement statement) {
        final List<FrameworkMethod> afters = getTestClass().getAnnotatedMethods(AfterClass.class);
        return new RunAfters(statement, afters, null) {
            @Override
            public void evaluate() throws Throwable {
                super.evaluate();
                cleanupInjector();
            }
        };
    }

    private void cleanupMocks() {
        for (Object mock : mocksToReset) {
            Mockito.reset(mock);
        }
    }

    private void cleanupInjector() {
        injector.shutdown();
    }

    private void prepareTestInstance(Object testInstance) {
        injector.injectMembers(testInstance);
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
                throw new RuntimeException("Error instantiating module " + moduleClass
                        + ". Please ensure that the module is public and has a no-arg constructor", e);
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

    private class CopyBindingTargetVisitor<T> implements BindingTargetVisitor<T, Void> {
        
        private LinkedBindingBuilder builder;

        public CopyBindingTargetVisitor(LinkedBindingBuilder builder) {
            this.builder = builder;
        }

        @Override
        public Void visit(InstanceBinding<? extends T> binding) {
            builder.toInstance(binding.getInstance());
            return null;
        }

        @Override
        public Void visit(ProviderInstanceBinding<? extends T> binding) {
            builder.toProvider(binding.getProviderInstance()).in(Scopes.SINGLETON);
            return null;
        }

        @Override
        public Void visit(ProviderKeyBinding<? extends T> binding) {
            builder.toProvider(binding.getProviderKey()).in(Scopes.SINGLETON);
            return null;
        }

        @Override
        public Void visit(LinkedKeyBinding<? extends T> binding) {
            builder.to(binding.getLinkedKey()).in(Scopes.SINGLETON);
            return null;
        }

        @Override
        public Void visit(ExposedBinding<? extends T> binding) {
            builder.to(binding.getKey()).in(Scopes.SINGLETON);
            return null;
        }

        @Override
        public Void visit(UntargettedBinding<? extends T> binding) {
            builder.to(binding.getKey().getTypeLiteral().getRawType()).in(Scopes.SINGLETON);
            return null;
        }

        @Override
        public Void visit(ConstructorBinding<? extends T> binding) {
            builder.toConstructor((Constructor<T>) binding.getConstructor().getMember()).in(Scopes.SINGLETON);
            return null;
        }

        @Override
        public Void visit(ConvertedConstantBinding<? extends T> binding) {
            builder.toInstance(binding.getValue());
            return null;
        }

        @Override
        public Void visit(ProviderBinding<? extends T> binding) {
            builder.toProvider(binding.getProvider()).in(Scopes.SINGLETON);
            return null;
        }
    }
}