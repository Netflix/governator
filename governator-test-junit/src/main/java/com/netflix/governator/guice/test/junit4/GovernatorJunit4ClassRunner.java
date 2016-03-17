package com.netflix.governator.guice.test.junit4;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
import com.google.inject.Module;
import com.google.inject.binder.AnnotatedBindingBuilder;
import com.google.inject.binder.LinkedBindingBuilder;
import com.google.inject.name.Names;
import com.google.inject.util.Modules;
import com.netflix.governator.InjectorBuilder;
import com.netflix.governator.LifecycleInjector;
import com.netflix.governator.guice.ModulesForTesting;
import com.netflix.governator.guice.test.ReplaceWithMock;

public class GovernatorJunit4ClassRunner extends BlockJUnit4ClassRunner {

    private final LifecycleInjector injector;
    private final List<Object> mocks = new ArrayList<>();

    public GovernatorJunit4ClassRunner(Class<?> klass) throws InitializationError {
        super(klass);
        injector = createInjector(getModulesForTestClass(klass), getOverridesForTestClass(klass));
    }

    @Override
    protected Object createTest() throws Exception {
        final Object testInstance = super.createTest();
        prepareTestInstance(testInstance);
        return testInstance;
    }
    
    @Override
    protected Statement withAfters(FrameworkMethod method, Object target, Statement statement) {
        final List<FrameworkMethod> afters = getTestClass().getAnnotatedMethods(
                After.class);
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
       for(Object mock : mocks) {
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
        return InjectorBuilder
                .fromModules(
                        Modules.override(modules)
                        .with(overrides))
                .createInjector();
    }

    private List<Module> getModulesForTestClass(Class<?> testClass) {
        final List<Class<? extends Module>> moduleClasses = new ArrayList<>();
        final List<Module> modules = new ArrayList<>();

        moduleClasses.addAll(getModulesForAnnotatedClass(testClass));
        for (Class<?> parentClass : ClassUtils.getAllSuperclasses(testClass)) {
            moduleClasses.addAll(getModulesForAnnotatedClass(parentClass));
        }

        for (Class<? extends Module> moduleClass : moduleClasses) {
            try {
                modules.add(moduleClass.newInstance());
            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException("Error instantiating module " + moduleClass
                        + ". Please ensure that the module is public and has a no-arg constructor", e);
            }
        }

        return modules;
    }

    private List<Class<? extends Module>> getModulesForAnnotatedClass(Class<?> clazz) {
        final Annotation annotation = clazz.getAnnotation(ModulesForTesting.class);
        if (annotation != null) {
            return Arrays.asList(((ModulesForTesting) annotation).value());
        } else {
            return Collections.emptyList();
        }
    }
    
    private List<Module> getOverridesForTestClass(Class<?> testClass) {
        final List<Module> overrides = new ArrayList<>();
        overrides.addAll(getOverridesForAnnotatedFields(testClass));
        for (Class<?> parentClass : ClassUtils.getAllSuperclasses(testClass)) {
            overrides.addAll(getOverridesForAnnotatedFields(parentClass));
        }

        return overrides;
    }
    
    @SuppressWarnings("rawtypes")
    private List<Module> getOverridesForAnnotatedFields(Class<?> clazz) {
        final List<Module> overrides = new ArrayList<>();
        for (Field field : clazz.getDeclaredFields()) {
            if (field.isAnnotationPresent(ReplaceWithMock.class)) {
                overrides.add(new MockitoOverrideModule(field.getAnnotation(ReplaceWithMock.class), field.getType()));
            }
        }
        return overrides;
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
            mocks.add(mock);
            LinkedBindingBuilder<T> bindingBuilder = bind(classToBind);
            if(!annotation.name().isEmpty()) {
                bindingBuilder = ((AnnotatedBindingBuilder<T>)bindingBuilder).annotatedWith(Names.named(annotation.name()));
            }
            bindingBuilder.toInstance(mock);
        }
    }
}
