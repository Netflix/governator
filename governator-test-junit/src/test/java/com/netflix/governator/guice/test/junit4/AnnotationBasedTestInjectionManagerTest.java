package com.netflix.governator.guice.test.junit4;

import org.junit.Test;

import com.google.inject.AbstractModule;
import com.netflix.governator.guice.test.AnnotationBasedTestInjectorManager;
import com.netflix.governator.guice.test.ModulesForTesting;

public class AnnotationBasedTestInjectionManagerTest {
    
    @Test(expected=RuntimeException.class)
    public void testExceptionThrownWhenModuleWithNoDefaultConstructorProvided() {
        new AnnotationBasedTestInjectorManager(TestClassForModulesWithoutDefaultConstrutor.class);
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
