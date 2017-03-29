package com.netflix.governator.guice.test;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.netflix.governator.guice.test.AnnotationBasedTestInjectorManager;
import com.netflix.governator.guice.test.ModulesForTesting;
import com.netflix.governator.guice.test.mocks.MockHandler;

public class AnnotationBasedTestInjectionManagerTest {
    
    @Test(expected=RuntimeException.class)
    public void testExceptionThrownWhenModuleWithNoDefaultConstructorProvided() {
        new AnnotationBasedTestInjectorManager(TestClassForModulesWithoutDefaultConstrutor.class, TestDefaultdMockHandler.class);
    }
    
    @Test
    public void testMockHandlerSelection() {
        AnnotationBasedTestInjectorManager annotationBasedTestInjectorManager = new AnnotationBasedTestInjectorManager(ParentTest.class, TestDefaultdMockHandler.class);
        assertTrue(annotationBasedTestInjectorManager.getMockHandler() instanceof TestParentMockHandler);
    }
    
    @Test
    public void testMockHandlerOverridenByChild() {
        AnnotationBasedTestInjectorManager annotationBasedTestInjectorManager = new AnnotationBasedTestInjectorManager(ChildTest.class, TestDefaultdMockHandler.class);
        assertTrue(annotationBasedTestInjectorManager.getMockHandler() instanceof TestChildMockHandler);
    }
    
    @Test
    public void testMockHandlerInheritence() {
        AnnotationBasedTestInjectorManager annotationBasedTestInjectorManager = new AnnotationBasedTestInjectorManager(InheritectMockHandlerTest.class, TestDefaultdMockHandler.class);
        assertTrue(annotationBasedTestInjectorManager.getMockHandler() instanceof TestParentMockHandler);
    }

}

@ModulesForTesting(ModuleWithoutDefaultConstructor.class)
class TestClassForModulesWithoutDefaultConstrutor {
    
}

class ModuleWithoutDefaultConstructor extends AbstractModule {
   
    public ModuleWithoutDefaultConstructor(String someArg) {}
    
    @Override
    protected void configure() {}
}

@ModulesForTesting(mockHandler=TestParentMockHandler.class)
class ParentTest {
    
}

@ModulesForTesting(mockHandler=TestChildMockHandler.class)
class ChildTest extends ParentTest {
    
}

@ModulesForTesting
class InheritectMockHandlerTest extends ParentTest {
    
}

class TestDefaultdMockHandler implements MockHandler {
    public <T> T createMock(Class<T> classToMock) {
        return null;
    }
    public <T> T createMock(Class<T> classToMock, Object args) {
        return null;
    }
    public <T> T createSpy(T objectToSpy) {
        return null;
    }
    public void resetMock(Object mockToReset) {}
}

class TestChildMockHandler implements MockHandler {
    public <T> T createMock(Class<T> classToMock) {
        return null;
    }
    public <T> T createMock(Class<T> classToMock, Object args) {
        return null;
    }
    public <T> T createSpy(T objectToSpy) {
        return null;
    }
    public void resetMock(Object mockToReset) {}
}

class TestParentMockHandler implements MockHandler {
    public <T> T createMock(Class<T> classToMock) {
        return null;
    }
    public <T> T createMock(Class<T> classToMock, Object args) {
        return null;
    }
    public <T> T createSpy(T objectToSpy) {
        return null;
    }
    public void resetMock(Object mockToReset) {}
}